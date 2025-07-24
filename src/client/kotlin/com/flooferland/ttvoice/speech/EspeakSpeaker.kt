package com.flooferland.ttvoice.speech

import com.flooferland.espeak.Espeak
import com.flooferland.ttvoice.TextToVoiceClient.Companion.LOGGER
import com.flooferland.ttvoice.VcPlugin
import com.flooferland.ttvoice.speech.ISpeaker.Status
import com.flooferland.ttvoice.util.Extensions.resampleRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class EspeakSpeaker : ISpeaker {
    var context: ISpeaker.WorldContext? = null

    override fun load(context: ISpeaker.WorldContext?) {
        Espeak.initialize(Espeak.AudioOutput.Retrieval, BUFFER_SIZE)
        LOGGER.info("Initialized eSpeak-NG v${Espeak.getVersion()}")
        this.context = context
    }

    override fun unload() {
        Espeak.terminate()
    }

    override fun speak(text: String): Status {
        // Adding the callback so I can get the data
        // TODO: Look into streaming the data into SVC directly from the callback
        val buffers = mutableListOf<ByteArray>()
        Espeak.setSynthCallback() { waveData, numberOfSamples, events, userData ->
            if (waveData != null && numberOfSamples > 0) {
                val bytes = waveData.getByteArray(0, numberOfSamples * 2)
                buffers.add(bytes)
            }
            return@setSynthCallback 0
        }
        Espeak.synth(text)

        // Combining the output data, now that we got it
        val bytes = buffers.fold(ByteArray(0)) { acc, chunk -> acc + chunk }

        // Bytes to ShortArray (I hate endianness)
        val rawPcm = ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .run { ShortArray(remaining()).also(::get) }

        // Resampling because eSpeak has a silly default sample rate
        // TODO: Look into compiling eSpeak myself and changing its sample rate to skip this step
        val pcm = rawPcm.resampleRate(INPUT_SAMPLERATE, OUTPUT_SAMPLERATE)

        // Playing locally on the client
        // TODO: Figure out why this isn't working
        CoroutineScope(Dispatchers.Main).launch {
            val buffer = ByteBuffer.allocate(pcm.size * Short.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
            pcm.forEach { buffer.putShort(it) }

            val format = AudioFormat(
                OUTPUT_SAMPLERATE.toFloat(), 16, 1,
                true, false
            )
            val info = DataLine.Info(SourceDataLine::class.java, format)
            val line = AudioSystem.getLine(info) as SourceDataLine
            line.open(format)
            line.start()
            line.write(bytes, 0, buffer.array().size)
            line.drain()
            line.stop()
            line.close()
        }

        // Streaming the data to Simple Voice Chat
        CoroutineScope(Dispatchers.IO).launch {
            pcm.asSequence()
                .chunked(FRAME_SAMPLES)
                .forEach { frame ->
                    VcPlugin.channel?.play(frame.toShortArray())
                    delay(FRAME_MS.toLong())
                }
        }
        return Status.Success()
    }

    override fun shutUp() {
        Espeak.cancel()
    }

    override fun isSpeaking(): Boolean {
        return Espeak.isPlaying()
    }

    companion object {
        const val INPUT_SAMPLERATE = 25_050
        const val OUTPUT_SAMPLERATE = 48_000
        const val FRAME_MS = 20
        const val FRAME_SAMPLES = (OUTPUT_SAMPLERATE * FRAME_MS) / 1_000
        const val BUFFER_SIZE = FRAME_MS * 3 // frames
    }
}
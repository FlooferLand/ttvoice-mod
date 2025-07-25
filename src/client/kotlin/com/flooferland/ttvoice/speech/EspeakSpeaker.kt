package com.flooferland.ttvoice.speech

import com.flooferland.espeak.Espeak
import com.flooferland.ttvoice.TextToVoiceClient.Companion.LOGGER
import com.flooferland.ttvoice.VcPlugin
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.speech.ISpeaker.Status
import com.flooferland.ttvoice.util.Extensions.resampleRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.Clip
import javax.sound.sampled.Mixer

// TODO: Unify things more. Make the buffer that gets sent to SVC play at about the same time as the buffer that gets sent to the client device.
//       Right now there are some delays/inconsistencies between what the other players hear and what the player hear

class EspeakSpeaker : ISpeaker {
    var context: ISpeaker.WorldContext? = null
    var speakerJob: Job? = null
    var runningSpeakerJob = false

    override fun load(context: ISpeaker.WorldContext?): Result<EspeakSpeaker> {
        val result = Espeak.initialize(Espeak.AudioOutput.Retrieval, BUFFER_SIZE)
        if (result.isSuccess) {
            LOGGER.info("Initialized eSpeak-NG v${Espeak.getVersion()}")
            this.context = context
            EspeakSpeaker.sampleRate = result.getOrNull()!!
            return Result.success(this)
        } else {
            return Result.failure(Error("Error initializing eSpeak-NG v${Espeak.getVersion()}"))
        }
    }

    override fun unload() {
        Espeak.terminate()
    }

    override fun speak(text: String): Status {
        speakerJob?.cancel()

        // Adding the callback so I can get the data
        // TODO: Look into streaming the data into SVC directly from the callback
        val buffers = mutableListOf<ByteArray>()
        Espeak.setSynthCallback() { waveData, numberOfSamples, events ->
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
        val pcm = rawPcm.resampleRate(sampleRate, OUTPUT_SAMPLERATE)

        // Parent speaker task (required to tell when speaking or not)
        speakerJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val children = mutableListOf<Job>()

            // Playing locally on the client
            if (ModState.config.general.hearSelf) {
                children += CoroutineScope(Dispatchers.IO).launch {
                    val caught = runCatching {
                        playToDevice(pcm)
                    }
                    caught.onFailure { err ->
                        LOGGER.error(err.message, err.cause)
                    }
                }
            }

            // Playing through Voicemeeter / external
            if (ModState.config.general.routeThroughDevice) {
                children += CoroutineScope(Dispatchers.IO).launch {
                    val device = AudioSystem.getMixerInfo().getOrNull(ModState.config.audio.device)
                    val caught = runCatching {
                        playToDevice(pcm, device)
                    }
                    caught.onFailure { err ->
                        LOGGER.error(err.message, err.cause)
                    }
                }
            }

            // Streaming the data to Simple Voice Chat
            if (ModState.config.general.routeThroughVoiceChat && VcPlugin.connected) {
                children += CoroutineScope(Dispatchers.IO).launch {
                    val chunks = pcm.asSequence().chunked(FRAME_SAMPLES).toList()
                    chunks.forEachIndexed { i, frame ->
                        if (!isSpeaking()) return@forEachIndexed
                        VcPlugin.channel?.play(frame.toShortArray())
                        delay(FRAME_MS.toLong())
                    }
                }
            }

            // Waiting for all yapping tasks to finish
            runningSpeakerJob = true
            children.joinAll()
            runningSpeakerJob = false
        }
        speakerJob?.invokeOnCompletion {
            runningSpeakerJob = false
        }

        return Status.Success()
    }

    override fun shutUp() {
        Espeak.cancel()
        speakerJob?.cancelChildren()
        speakerJob?.cancel()
        runningSpeakerJob = false
    }

    override fun isSpeaking(): Boolean {
        return runningSpeakerJob
    }

    suspend fun playToDevice(pcm: ShortArray, device: Mixer.Info? = null) {
        val pcmBytes = ByteBuffer.allocate(pcm.size * Short.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .also { buf -> pcm.forEach { buf.putShort(it) } }
            .array()
        val dataFormat = AudioFormat(
            OUTPUT_SAMPLERATE.toFloat(), 16, 1,
            true, false
        )
        val stream = AudioInputStream(
            ByteArrayInputStream(pcmBytes),
            dataFormat,
            (pcmBytes.size / dataFormat.frameSize).toLong()
        )
        val mixer = AudioSystem.getMixer(device)  // null = default mixer
        val info = DataLine.Info(Clip::class.java, null)
        val clip = mixer.getLine(info) as Clip
        val targetFormats = AudioSystem.getTargetFormats(dataFormat.encoding, dataFormat)
            .filter { AudioSystem.isLineSupported(DataLine.Info(Clip::class.java, it)) }
        if (targetFormats.isEmpty()) {
            error("No converter from $dataFormat to device format")
        }
        val target = targetFormats.first()
        val converted = AudioSystem.getAudioInputStream(target, stream)
        clip.open(converted)
        clip.start()
        delay(FRAME_MS.toLong())
        while (true) {
            if (clip.isRunning && isSpeaking()) break
            delay(FRAME_MS.toLong())
        }
        clip.close()
        stream.close()
    }

    companion object {
        const val OUTPUT_SAMPLERATE = 48_000
        const val FRAME_MS = 20
        const val FRAME_SAMPLES = (OUTPUT_SAMPLERATE * FRAME_MS) / 1_000
        const val BUFFER_SIZE = FRAME_MS * 3 // frames
        var sampleRate = -1 // filled by eSpeak
    }
}
package com.flooferland.ttvoice.speech

import com.flooferland.espeak.Espeak
import com.flooferland.ttvoice.TextToVoiceClient
import com.flooferland.ttvoice.TextToVoiceClient.Companion.LOGGER
import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.VcPlugin
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.figura.FiguraEventPlugin
import com.flooferland.ttvoice.speech.ISpeaker.Status
import com.flooferland.ttvoice.util.Extensions.resampleRate
import com.flooferland.ttvoice.util.SatisfyingNoises
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine

// TODO: Unify things more. Make the buffer that gets sent to SVC play at about the same time as the buffer that gets sent to the client device.
//       Right now there are some delays/inconsistencies between what the other players hear and what the player hear

class EspeakSpeaker : ISpeaker {
    var context: ISpeaker.WorldContext? = null
    val activeJobs = Collections.synchronizedList(mutableListOf<SpeechJob>())

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

        // Starting speech output
        val speech = SpeechJob(pcm, context)
        speech.start() {
            activeJobs.remove(speech)
            println("Removing job")
        }
        activeJobs.add(speech)

        return Status.Success()
    }

    override fun shutUp() {
        Espeak.cancel()
        activeJobs.forEach { it.cancel() }
    }

    override fun isSpeaking(): Boolean {
        return activeJobs.isNotEmpty()
    }

    class SpeechJob(val pcm: ShortArray, val context: ISpeaker.WorldContext?) {
        var running = AtomicBoolean(true)
        var playhead = 0
        var endCallback: (() -> Unit)? = null

        fun start(onEnd: (() -> Unit)? = null) {
            endCallback = onEnd
            CoroutineScope(Dispatchers.IO).launch {
                val errHandler = { err: Throwable ->
                    sendError(
                        context,
                        "An error occurred opening the audio device. The audio will still play through Simple Voice Chat, but try changing your main audio device's sample rate or opening an issue on the GitHub repository",
                        (err.message ?: err.cause ?: err.toString()) as String
                    )
                }

                val deviceInfo =
                    if (ModState.config.general.routeThroughDevice) {
                        AudioSystem.getMixerInfo().getOrNull(ModState.config.audio.device)
                    } else {
                        null
                    }
                val deviceResult = deviceInfo?.let { getDevice(it) }
                deviceResult?.onFailure(errHandler)
                val device = deviceResult?.getOrNull()

                while (running.get()) {
                    val frame = nextFrame()
                    if (frame == null) break
                    val bytes = pcmAsBytes(frame)

                    // Playing through another device (Voicemeeter, etc)
                    device?.write(bytes, 0, bytes.size)

                    // Streaming the data to Simple Voice Chat
                    if (ModState.config.general.routeThroughVoiceChat && VcPlugin.connected && !SpeechUtil.isTestingArmed()) {
                        VcPlugin.sendFrame(frame)
                    }

                    // Streaming to Figura
                    if (TextToVoiceClient.isFiguraInstalled) {
                        FiguraEventPlugin.sendSpeakingEvent(frame)
                    }

                    // Delay
                    val frameDelayNs = (FRAME_MS - FRAME_MS_STITCH) * 1_000_000L
                    val nextFrameTime = (System.nanoTime() + frameDelayNs)
                    while (System.nanoTime() < nextFrameTime) yield()
                }

                running.set(false)
                device?.close()
                endCallback?.invoke()
                playhead = 0
            }
        }

        fun cancel() {
            running.set(false)
        }

        // TODO: Fix nexFrame; Currently, the playhead is always larger than pcm.size
        fun nextFrame(): ShortArray? {
            if (playhead > pcm.size || !running.get()) return null
            val end = (playhead + BUFFER_SIZE).coerceAtMost(pcm.size)
            val chunk = pcm.sliceArray(playhead until end)
            playhead += BUFFER_SIZE
            return chunk
        }

        /** Gets the source data line; Will return an error if Java's audio system is acting up, as it always does */
        fun getDevice(device: Mixer.Info? = null): Result<SourceDataLine> {
            val bestFormat = AudioFormat(
                OUTPUT_SAMPLERATE.toFloat(), 16, 1,
                true, false
            )

            // Finding a line
            val result = runCatching { AudioSystem.getSourceDataLine(bestFormat, device) }
            result.onFailure { err ->
                return Result.failure(err)
            }

            // Starting the line
            val line = result.getOrNull()!!
            val lineResult = runCatching {
                line.open(bestFormat, BUFFER_SIZE)
                line.start()
            }
            lineResult.onSuccess {
                return Result.success(line)
            }
            lineResult.onFailure { err ->
                return Result.failure(Error("Error opening line: $err"))
            }
            return Result.failure(Error("Unknown"))
        }

        fun pcmAsBytes(data: ShortArray): ByteArray {
            return ByteBuffer.allocate(data.size * Short.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .also { buf -> data.forEach { buf.putShort(it) } }
                .array()
        }

        fun sendError(context: ISpeaker.WorldContext?, text: String, details: String) {
            context?.player?.sendMessage(
                Text.literal("$MOD_ID error: $text")
                    .formatted(Formatting.RED, Formatting.BOLD, Formatting.UNDERLINE)
                    .setStyle(Style.EMPTY.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(details))))
            )
            LOGGER.error("$text ($details)")
            SatisfyingNoises.playDeny()
        }
    }

    companion object {
        /** Target sample rate for SVC: https://modrepo.de/minecraft/voicechat/api/examples */
        const val OUTPUT_SAMPLERATE = 48_000

        // TODO: Separate SVC from this value by manually re-chunking the data once it reaches SVC
        /** How long each frame should last; Must be 20 for SVC to work correctly */
        const val FRAME_MS = 20

        /** Stitch frames to prevent harsh cuts; Should be way less than FRAME_MS */
        const val FRAME_MS_STITCH = 0

        /** The size of one audio chunk */
        const val BUFFER_SIZE = (OUTPUT_SAMPLERATE * FRAME_MS) / 1_000

        /** Input sample rate, filled by eSpeak */
        var sampleRate = -1
    }
}
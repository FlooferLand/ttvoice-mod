package com.flooferland.ttvoice.speech

import com.flooferland.ttvoice.TextToVoiceClient.LOGGER
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.data.TextToVoiceConfig
import com.flooferland.ttvoice.speech.ISpeaker.Status
import com.flooferland.ttvoice.speech.ISpeaker.StatusType
import java.util.concurrent.LinkedBlockingQueue
import javax.sound.sampled.AudioSystem

class SpeechThread(val context: ISpeaker.WorldContext?) : Thread() {
    private var queue = LinkedBlockingQueue<ICommand>()
    private var errorQueue = LinkedBlockingQueue<(Status.Failure) -> Unit>()
    private var speaker: ISpeaker? = null

    companion object {
        public const val DEFAULT_VOICE: String = "gmw\\en-US"
    }

    /** Only set after run is called */
    var defaultVoice: String = DEFAULT_VOICE

    interface ICommand
    data class SpeakCommand(val text: String, val monophonic: Boolean = false) : ICommand
    class ShutUpCommand : ICommand
    class StopThreadCommand : ICommand

    override fun run() {
        val backend = spawnBackend().load(context)
        backend.onSuccess { backend ->
            speaker = backend
            if (speaker is EspeakSpeaker) {
                defaultVoice = (speaker as EspeakSpeaker).defaultVoice ?: defaultVoice
            }
        }
        backend.onFailure { err ->
            // TODO: Switch backends if one doesn't work
            emitError(Status.Failure(ISpeaker.StatusType.Internal, err.toString()))
            return
        }

        while (true) {
            val task = queue.take()
            when (task) {
                is SpeakCommand -> {
                    if (task.monophonic) {
                        speaker?.shutUp()
                    }
                    val status = speak(task.text)
                    if (status is Status.Failure) {
                        emitError(status)
                    }
                }

                is ShutUpCommand -> {
                    speaker?.shutUp()
                }

                is StopThreadCommand -> {
                    speaker?.unload()
                    break
                }
            }
        }
    }

    private fun speak(text: String): Status {
        // Mixer parsing
        if (ModState.config.general.routeThroughDevice) {
            var selectedMixer = -1
            val allMixers = AudioSystem.getMixerInfo()
            for (i in allMixers.indices) {
                if (i == ModState.config.audio.device) {
                    selectedMixer = i
                    break
                }
            }
            if (selectedMixer == -1) {
                return Status.Failure(StatusType.NoOutputDevice, "Selected mixer doesn't exist")
            }
        }

        // No routing warning
        if (!ModState.config.general.let { it.routeThroughVoiceChat || it.routeThroughDevice }) {
            return Status.Failure(StatusType.NoOutputDevice, "No output set. Make sure to route through at least one output in the mod config")
        }

        // Generating the TTS audio and playing it
        if (speaker == null) {
            return Status.Failure(StatusType.Internal, "No speaker was never initialized (speaker == null)")
        }
        val result = runCatching { speaker?.speak(text) };
        result.onFailure { err -> return Status.Failure(StatusType.Internal, err.message.toString()) }
        result.onSuccess { info -> return Status.Success() }
        return Status.Failure(StatusType.Internal, "Unknown; Did not succeed")
    }

    public fun send(command: ICommand) {
        queue.add(command)
    }

    public fun onError(callback: (Status.Failure) -> Unit) {
        errorQueue.add(callback)
    }

    fun isSpeaking() = speaker?.isSpeaking()

    private fun emitError(error: Status.Failure) {
        if (errorQueue.isNotEmpty()) {
            val callback = errorQueue.take()
            callback.invoke(error)
            LOGGER.error("Error ${error.type} in speech thread: ${error.context}")
        }
    }

    private fun spawnBackend(): ISpeaker {
        return when (ModState.config.audio.ttsBackend) {
            TextToVoiceConfig.TTSBackend.Espeak -> EspeakSpeaker()
        }
    }
}
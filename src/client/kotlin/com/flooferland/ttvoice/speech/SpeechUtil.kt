package com.flooferland.ttvoice.speech

import com.flooferland.ttvoice.TextToVoiceClient.Companion.LOGGER
import com.flooferland.ttvoice.VcPlugin
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.data.TextToVoiceConfig
import com.flooferland.ttvoice.screen.SelectDeviceScreen
import javax.sound.sampled.AudioSystem
import com.flooferland.ttvoice.speech.ISpeaker.Status
import com.flooferland.ttvoice.speech.ISpeaker.StatusType
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.jetbrains.annotations.CheckReturnValue
import java.lang.Error

public object SpeechUtil : ISpeaker {
    private var loaded: ISpeaker? = null

    override fun load(context: ISpeaker.WorldContext?): Result<ISpeaker> {
        val speaker = spawnBackend().load(context)
        speaker.onSuccess {
            loaded = speaker.getOrNull()!!
            return Result.success(it)
        }
        speaker.onFailure { err ->
            // TODO: Switch backends if one doesn't work
            loaded = null
            return Result.failure(err)
        }
        loaded = null
        return Result.failure(Error("Speaker not found"))
    }

    override fun unload() {
        getBackend().unload()
    }

    @CheckReturnValue
    override fun speak(text: String): Status {
        val result = speakInternal(text)
        when (result) {
            is Status.Failure -> {
                val player = MinecraftClient.getInstance().player
                player?.sendMessage(Text.literal("Text-To-Voice Error ${result.type}:\n    ${result.context}").formatted(Formatting.RED))
            }
            else -> {}
        }
        return result
    }

    private fun speakInternal(text: String): Status {
        // Not loaded
        if (loaded == null) {
            return Status.Failure(StatusType.Internal, "Uninitialized")
        }

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

        // Voice chat
        if (ModState.config.general.routeThroughVoiceChat) {
            if (VcPlugin.muted)
                return Status.Failure(StatusType.VoiceMuted, "Can't play audio while muted")
            if (!VcPlugin.connected)
                return Status.Failure(StatusType.VoiceDisconnected, "Can't play audio while voice chat is disconnected or disabled")
        }

        // No routing warning
        if (!ModState.config.general.let { it.routeThroughVoiceChat || it.routeThroughDevice }) {
            return Status.Failure(StatusType.NoOutputDevice, "No output set. Make sure to route through at least one output in the mod config")
        }

        // Generating the TTS audio and playing it
        val result = runCatching { getBackend().speak(text) };
        result.onFailure { err -> return Status.Failure(StatusType.Internal, err.message.toString()) }
        result.onSuccess { info -> return Status.Success() }
        return Status.Failure(StatusType.Internal, "Unknown; Did not succeed")
    }

    override fun shutUp() {
        loaded?.shutUp()
    }

    fun playTest() {
        if (!isTestingArmed()) {
            LOGGER.warn("playTest was called, but audio testing was not armed")
            return
        }
        loaded?.speak("Audio test")
    }

    fun isTestingArmed(): Boolean {
        return MinecraftClient.getInstance().currentScreen is SelectDeviceScreen
    }

    override fun isSpeaking(): Boolean {
        return loaded?.isSpeaking() ?: false
    }

    fun getBackend(): ISpeaker {
        if (loaded == null) {
            loaded = spawnBackend()
        }
        return loaded!!
    }

    private fun spawnBackend(): ISpeaker {
        if (loaded != null) {
            loaded!!.unload()
            loaded = null
        }
        return when (ModState.config.audio.ttsBackend) {
            TextToVoiceConfig.TTSBackend.Espeak -> EspeakSpeaker()
        }
    }
}
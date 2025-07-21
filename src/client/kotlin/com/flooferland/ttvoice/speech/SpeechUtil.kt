package com.flooferland.ttvoice.speech

import com.flooferland.ttvoice.VcPlugin
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.data.TextToVoiceConfig
import javax.sound.sampled.AudioSystem

public object SpeechUtil : ISpeaker {
    override fun speak(text: String) {
        // Mixer parsing
        var selectedMixer = -1
        val allMixers = AudioSystem.getMixerInfo()
        for (i in allMixers.indices) {
            if (i == ModState.config.audio.device) {
                selectedMixer = i
                break
            }
        }
        if (selectedMixer == -1) {
            error("Selected mixer doesn't exist")
        }

        if (ModState.config.general.useSimpleVoiceChat) {
            if (VcPlugin.api.isMuted) {
                error("Can't play audio while muted")
            }
            if (VcPlugin.api.isDisconnected || VcPlugin.api.isDisabled) {
                error("Can't play audio while voice chat is disconnected or disabled")
            }
        }

        // Generating the TTS audio and playing it
        val speaker: ISpeaker = getBackend()
        val result = runCatching {
            speaker.speak(text)
        };
        result.onFailure { err ->
            error(err.message.toString())
        }
    }

    override fun shutUp() {
        getBackend().shutUp()
    }

    override fun playTest() {
        getBackend().playTest()
    }

    override fun isSpeaking(): Boolean {
        return getBackend().isSpeaking()
    }

    override fun load() {
        getBackend().load()
    }

    override fun unload() {
        getBackend().unload()
    }

    fun getBackend(): ISpeaker {
        return when (ModState.config.audio.ttsBackend) {
            TextToVoiceConfig.TTSBackend.Native -> {
                NativeSpeaker
            }
            TextToVoiceConfig.TTSBackend.Python -> {
                PythonSpeaker
            }
        }
    }
}
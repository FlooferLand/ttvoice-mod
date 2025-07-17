package com.flooferland.ttvoice.data

import com.flooferland.ttvoice.TextToVoiceClient
import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry
import javax.sound.sampled.AudioSystem

@Config(name = MOD_ID)
class ModConfig : ConfigData {
    var general: GeneralConfig = GeneralConfig()
    var audio: AudioConfig = AudioConfig()

    // Defs
    class GeneralConfig {
        var useSimpleVoiceChat: Boolean = true
        var broadcastToAudioMixer: Boolean = true

        @ConfigEntry.Gui.Tooltip
        var pythonPath: String = "C:/Users/FlooferLand/AppData/Local/Programs/Python/Python313/python.exe"
    }
    class AudioConfig {
        var device: Int = 18 // -1;  // TODO: ADD IN -1 AS DEFAULT; DEBUG ONLY
        var ttsBackend: TTSBackend = TTSBackend.Python
    }

    enum class TTSBackend {
        Native,
        Python,
        MaryTTS
    }

    override fun validatePostLoad() {
        if (audio.device < 0 || audio.device >= AudioSystem.getMixerInfo().size) {
            TextToVoiceClient.LOGGER.error("Audio device out of range in config. Resetting.")
            audio.device = 0
        }
    }
}
package com.flooferland.ttvoice.data

class TextToVoiceConfig {
    var general: GeneralConfig = GeneralConfig()
    var audio: AudioConfig = AudioConfig()

    // Defs
    class GeneralConfig {
        var useSimpleVoiceChat: Boolean = true
        var broadcastToAudioMixer: Boolean = true

        var pythonPath: String = "C:/Users/FlooferLand/AppData/Local/Programs/Python/Python313/python.exe"
    }
    class AudioConfig {
        var device: Int = -1;
        var ttsBackend: TTSBackend = TTSBackend.Python
        var uiSounds: Boolean = true
    }

    enum class TTSBackend {
        Native,
        Python
    }
}
package com.flooferland.ttvoice.data

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter

data class TextToVoiceConfig(
    var general: GeneralConfig = GeneralConfig(),
    var audio: AudioConfig = AudioConfig(),
    var ui: UiConfig = UiConfig()
) {
    // Types
    data class GeneralConfig(
        var useSimpleVoiceChat: Boolean = true,
        var broadcastToAudioMixer: Boolean = true,
        var pythonPath: String = "C:/Users/FlooferLand/AppData/Local/Programs/Python/Python313/python.exe"
    )
    data class AudioConfig(
        var device: Int = -1,
        var ttsBackend: TTSBackend = TTSBackend.Espeak
    )
    data class UiConfig(
        var viewHistory: Boolean = true,
        var sounds: Boolean = true
    )

    enum class TTSBackend {
        Espeak,
        Python
    }

    // Scuffed ass methods, but it's the only reliable way to do this
    fun clone(): TextToVoiceConfig {
        val configText = TomlWriter().write(this)
        return Toml().read(configText).to(TextToVoiceConfig::class.java)
    }
    fun compare(other: TextToVoiceConfig): Boolean {
        val current = TomlWriter().write(this)
        val other = TomlWriter().write(other)
        return (current == other)
    }
}
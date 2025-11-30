package com.flooferland.ttvoice.registry

import com.flooferland.ttvoice.TextToVoiceClient.MOD_ID
import com.flooferland.ttvoice.data.TextToVoiceConfig
import com.flooferland.ttvoice.data.ModState
import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object ModConfig {
    val configPath: Path = FabricLoader.getInstance().configDir.resolve("${MOD_ID}.toml")

    fun loadOrDefault() {
        if (!Files.exists(configPath)) {
            ModState.config = TextToVoiceConfig()
            return
        }

        val textResult = runCatching {
            configPath.toFile().readText()
        }
        textResult.onSuccess { configText ->
            val tomlResult = runCatching {
                ModState.config = Toml().read(configText).to(TextToVoiceConfig::class.java)
            }

            // Deserialization error
            tomlResult.onFailure { error ->
                ModState.config = TextToVoiceConfig()
                error.printStackTrace()
            }
        }

        // IO error
        textResult.onFailure { error ->
            ModState.config = TextToVoiceConfig()
        }
    }

    fun save() {
        // Serialization
        val toml = runCatching {
            TomlWriter().write(ModState.config, configPath.toFile())
        }
        toml.onFailure({ error ->
            error.printStackTrace()
        })
    }
}
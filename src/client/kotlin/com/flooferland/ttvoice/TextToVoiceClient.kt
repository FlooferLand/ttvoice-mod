package com.flooferland.ttvoice

import com.flooferland.ttvoice.data.ModConfig
import com.flooferland.ttvoice.registry.ModCommands
import com.flooferland.ttvoice.registry.ModEvents
import com.flooferland.ttvoice.registry.ModKeybinds
import com.flooferland.ttvoice.registry.ModResources
import com.flooferland.ttvoice.util.ModState
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer
import net.fabricmc.api.ClientModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TextToVoiceClient : ClientModInitializer {
    override fun onInitializeClient() {
        // Registering config
        AutoConfig.register(ModConfig::class.java, ::Toml4jConfigSerializer);
        ModState.config = AutoConfig.getConfigHolder(ModConfig::class.java).config

        // Registering other things
        ModResources.registerReloaders()
        ModKeybinds.registerKeybinds()
        ModEvents.registerEvents()
        ModCommands.registerCommands()
    }

    companion object {
        const val MOD_ID: String = "ttvoice"
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    }
}
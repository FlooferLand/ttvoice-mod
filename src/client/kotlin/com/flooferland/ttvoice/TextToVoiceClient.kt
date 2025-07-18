package com.flooferland.ttvoice

import com.flooferland.ttvoice.registry.ModCommands
import com.flooferland.ttvoice.registry.ModConfig
import com.flooferland.ttvoice.registry.ModEvents
import com.flooferland.ttvoice.registry.ModKeybinds
import com.flooferland.ttvoice.registry.ModResources
import net.fabricmc.api.ClientModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TextToVoiceClient : ClientModInitializer {
    override fun onInitializeClient() {
        // Registering config
        ModConfig.loadOrDefault()

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
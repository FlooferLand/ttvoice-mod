package com.flooferland.ttvoice

import com.flooferland.ttvoice.loader.fabric.LoaderUtils
import com.flooferland.ttvoice.registry.ModCommands
import com.flooferland.ttvoice.registry.ModConfig
import com.flooferland.ttvoice.registry.ModEvents
import com.flooferland.ttvoice.registry.ModKeybinds
import java.nio.file.Files
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object TextToVoiceClient {
    const val MOD_ID: String = "ttvoice"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    val dataDir = LoaderUtils.getDataDir()
    val isFiguraInstalled = LoaderUtils.isFiguraInstalled()

    fun onInitializeClient() {
        // Setting up mod data files
        Files.createDirectories(dataDir)

        // Registering config
        ModConfig.loadOrDefault()

        // Registering other things
        ModKeybinds.registerKeybinds()
        ModEvents.registerEvents()
        ModCommands.registerCommands()
    }
}
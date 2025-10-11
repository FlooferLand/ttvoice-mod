package com.flooferland.ttvoice

import com.flooferland.ttvoice.registry.ModCommands
import com.flooferland.ttvoice.registry.ModConfig
import com.flooferland.ttvoice.registry.ModEvents
import com.flooferland.ttvoice.registry.ModKeybinds
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files

public class TextToVoiceClient : ClientModInitializer {
    override fun onInitializeClient() {
        isFiguraInstalled = FabricLoader.getInstance().isModLoaded("figura")

        // Setting up mod data files
        Files.createDirectories(dataDir)

        // Registering config
        ModConfig.loadOrDefault()

        // Registering other things
        ModKeybinds.registerKeybinds()
        ModEvents.registerEvents()
        ModCommands.registerCommands()
    }

    companion object {
        const val MOD_ID: String = "ttvoice"
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
        val dataDir = FabricLoader.getInstance().gameDir.resolve(MOD_ID)
        var isFiguraInstalled: Boolean = false
    }
}
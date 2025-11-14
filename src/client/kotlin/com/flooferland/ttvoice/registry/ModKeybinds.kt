package com.flooferland.ttvoice.registry

import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.screen.SpeechScreen
import com.flooferland.ttvoice.util.rl
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

enum class ModKeybinds(translationKey: String, code: InputConstants.Type, key: Int) {
    SpeakBinding(
        "key.${MOD_ID}.speak",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_GRAVE_ACCENT,
    );

    val bind = KeyBindingHelper.registerKeyBinding(
        //? if >1.21.1 {
        /*KeyMapping(translationKey, code, key, KeyMapping.Category.register(rl("general")))
        *///?} else {
        KeyMapping(translationKey, code, key, "category.${MOD_ID}.general")
        //?}
    )!!

    companion object {
        fun registerKeybinds() {
            ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
                // Opening the speak screen
                while (ModKeybinds.SpeakBinding.bind.consumeClick()) {
                    Minecraft.getInstance().setScreen(SpeechScreen())
                }
            })
        }
    }
}
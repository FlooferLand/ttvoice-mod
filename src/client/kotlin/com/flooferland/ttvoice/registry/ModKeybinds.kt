package com.flooferland.ttvoice.registry

import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.screen.SpeechScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

enum class ModKeybinds(translationKey: String, code: InputUtil.Type, key: Int) {
    SpeakBinding(
        "key.${MOD_ID}.speak",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_GRAVE_ACCENT,
    );

    val bind: KeyBinding = KeyBindingHelper.registerKeyBinding(
        //? if >1.21.1 {
        /*KeyBinding(translationKey, code, key, KeyBinding.Category.create(Identifier.of(MOD_ID, "general")))
        *///?} else {
        KeyBinding(translationKey, code, key, "category.${MOD_ID}.general")
        //?}
    )

    companion object {
        fun registerKeybinds() {
            ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
                // Opening the speak screen
                while (ModKeybinds.SpeakBinding.bind.wasPressed()) {
                    MinecraftClient.getInstance().setScreen(SpeechScreen())
                }
            })
        }
    }
}
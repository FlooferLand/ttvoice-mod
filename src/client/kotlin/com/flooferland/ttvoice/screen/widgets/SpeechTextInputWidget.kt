package com.flooferland.ttvoice.screen.widgets

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

public class SpeechTextInputWidget(textRenderer: TextRenderer, x: Int, y: Int, width: Int, height: Int, text: Text, val actionOnPress: () -> Unit)
    : TextFieldWidget(textRenderer, x, y, width, height, text)
{
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val pressed = super.keyPressed(keyCode, scanCode, modifiers)
        if (pressed && keyCode == GLFW.GLFW_KEY_ENTER) {
            actionOnPress.invoke()
        }
        return pressed
    }
}
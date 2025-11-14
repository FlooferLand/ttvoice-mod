package com.flooferland.ttvoice.screen.widgets

import com.flooferland.ttvoice.screen.SpeechScreen
import com.flooferland.ttvoice.util.SatisfyingNoises
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.lwjgl.glfw.GLFW

public class SpeechTextInputWidget(val screen: SpeechScreen, font: Font, x: Int, y: Int, width: Int, height: Int, text: Component, val actionOnPress: () -> Unit)
    : EditBox(font, x, y, width, height, text)
{
    val errStyle = Style.EMPTY.applyFormat(ChatFormatting.DARK_RED)!!
    val cmdStyle = Style.EMPTY.applyFormat(ChatFormatting.BLUE)!!
    val cmdArgStyle = Style.EMPTY.applyFormats(ChatFormatting.DARK_GRAY, ChatFormatting.UNDERLINE)!!

    init {
        setMaxLength(256)
        
        // TODO: Add syntax highlighting back in for modern versions
        //? if <1.21.9 {
        super.setFormatter() { str, i -> textRenderProvider(str, i).visualOrderText }
        //?} else {
        //?}
    }

    fun textRenderProvider(str: String, i: Int): Component {
        // Commands
        if (str.firstOrNull() == '/') {
            val tokens = str.split(' ')
            if (tokens.isEmpty())
                return Component.literal(str)

            val commandName = tokens[0].removePrefix("/")
            val text = Component.literal("/").setStyle(Style.EMPTY.withBold(true))
            text.append(Component.literal(commandName).setStyle(if (commandName in SpeechScreen.RecognizedCommands.commands()) cmdStyle else errStyle))
            for ((i, token) in tokens.withIndex()) {
                if (i == 0) continue
                text.append(" ")
                val tokenText = Component.literal(token)
                    .setStyle(cmdArgStyle)
                text.append(tokenText)
            }
            return text
        }

        // Normal text
        return Component.literal(str)
    }

    //? if >=1.21.9 {
    /*override fun keyPressed(input: net.minecraft.client.input.KeyEvent): Boolean {
        return isKeyPressed(input.key, input.scancode, input.modifiers)
    }
    *///?} else {
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return isKeyPressed(keyCode, scanCode, modifiers)
    }
    //?}

    fun isKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!isActive()) {
            return false;
        }

        // Manual handling
        var handled = false
        when (keyCode) {
            GLFW.GLFW_KEY_UP -> {
                if (value == "") {// Unclear

                } else if (screen.historyPointer > 0) {
                    screen.historyPointer -= 1

                    if (screen.historyScroll < screen.historyScrollMax) {
                        screen.historyScroll += 1
                    }
                } else {
                    screen.historyPointer = SpeechScreen.history.lastIndex
                    screen.historyScroll = 0
                }

                value = SpeechScreen.history.getOrNull(screen.historyPointer) ?: ""
                screen.updateHistoryWidget()
                handled = true
                SatisfyingNoises.playClick(1f)
            }

            GLFW.GLFW_KEY_DOWN -> {
                var clear = false
                if (screen.historyPointer < SpeechScreen.history.lastIndex) {
                    screen.historyPointer += 1

                    if (screen.historyScroll > 0) {
                        screen.historyScroll -= 1
                    }
                } else { // Clear the text for the most recent entry; easy way for the user to clear the textbox
                    if (modifiers != 0 || value.isEmpty()) {
                        screen.historyPointer = 0
                        screen.historyScroll = screen.historyScrollMax
                    } else {
                        clear = true
                        value = ""
                    }
                }

                value = if (clear) ""
                else SpeechScreen.history.getOrNull(screen.historyPointer) ?: ""
                screen.updateHistoryWidget(clear)
                handled = true
                SatisfyingNoises.playClick(-1f)
            }
            GLFW.GLFW_KEY_ENTER -> {
                actionOnPress.invoke()
                handled = true
            }
        }

        if (!handled) {
            //? if >=1.21.9 {
            /*handled = super.keyPressed(net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers))
            *///?} else {
            handled = super.keyPressed(keyCode, scanCode, modifiers)
            //?}
        }
        return handled
    }
}
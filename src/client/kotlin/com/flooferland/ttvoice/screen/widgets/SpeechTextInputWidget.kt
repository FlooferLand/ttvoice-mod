package com.flooferland.ttvoice.screen.widgets

import com.flooferland.ttvoice.TextToVoiceClient
import com.flooferland.ttvoice.screen.SpeechScreen
import com.flooferland.ttvoice.util.SatisfyingNoises
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.lwjgl.glfw.GLFW

public class SpeechTextInputWidget(val screen: SpeechScreen, textRenderer: TextRenderer, x: Int, y: Int, width: Int, height: Int, text: Text, val actionOnPress: () -> Unit)
    : TextFieldWidget(textRenderer, x, y, width, height, text)
{
    val errStyle: Style = Style.EMPTY.withFormatting(Formatting.DARK_RED)
    val cmdStyle: Style = Style.EMPTY.withFormatting(Formatting.BLUE)
    val cmdArgStyle: Style = Style.EMPTY.withFormatting(Formatting.DARK_GRAY, Formatting.UNDERLINE)

    init {
        setMaxLength(256)
        super.setRenderTextProvider() { str, i -> textRenderProvider(str, i).asOrderedText() }
    }

    fun textRenderProvider(str: String, i: Int): Text {
        // Commands
        if (str.firstOrNull() == '/') {
            val tokens = str.split(' ')
            if (tokens.isEmpty())
                return Text.of(str)

            val commandName = tokens[0].removePrefix("/")
            val text = Text.literal("/").setStyle(Style.EMPTY.withBold(true))
            text.append(Text.literal(commandName).setStyle(if (commandName in SpeechScreen.RecognizedCommands.commands()) cmdStyle else errStyle))
            for ((i, token) in tokens.withIndex()) {
                if (i == 0) continue
                text.append(" ")
                val tokenText = Text.literal(token)
                    .setStyle(cmdArgStyle)
                text.append(tokenText)
            }
            return text
        }

        // Normal text
        return Text.of(str)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!isActive()) {
            return false;
        }

        // Manual handling
        var handled = false
        when (keyCode) {
            GLFW.GLFW_KEY_UP -> {
                if (screen.historyPointer > 0) {
                    screen.historyPointer -= 1
                } else {
                    screen.historyPointer = SpeechScreen.history.lastIndex
                }
                text = SpeechScreen.history.getOrNull(screen.historyPointer) ?: ""
                screen.updateHistoryWidget()
                handled = true
                SatisfyingNoises.playClick(1f)
            }

            GLFW.GLFW_KEY_DOWN -> {
                var clear = false
                if (screen.historyPointer < SpeechScreen.history.lastIndex) {
                    screen.historyPointer += 1
                } else { // Clear the text for the most recent entry; easy way for the user to clear the textbox
                    if (modifiers != 0 || text.isEmpty()) {
                        screen.historyPointer = 0
                    } else {
                        clear = true
                        text = ""
                    }
                }
                text = if (clear) ""
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
            handled = super.keyPressed(keyCode, scanCode, modifiers)
        }
        return handled
    }
}
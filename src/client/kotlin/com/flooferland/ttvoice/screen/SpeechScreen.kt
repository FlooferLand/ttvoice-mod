package com.flooferland.ttvoice.screen

import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.screen.widgets.SpeechTextInputWidget
import com.flooferland.ttvoice.speech.SpeechUtil
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class SpeechScreen() : Screen(Text.of("Speech screen")) {
    lateinit var textBox: SpeechTextInputWidget
    lateinit var speakButton: ButtonWidget

    override fun init() {
        // Adding the voice text input textbox
        run {
            val size = Pair((width * 0.8).toInt(), (height * 0.2).toInt())
            textBox = SpeechTextInputWidget(
                textRenderer,
                (width * 0.5).toInt() - (size.first / 2),
                (height * 0.4).toInt() - (size.second / 2),
                size.first,
                size.second,
                Text.of("Input text here"),
                { speakButtonPressed(null); }
            )
            this.addDrawableChild(textBox)
        }

        // Button
        run {
            val size = Pair(200, 50)
            speakButton = ButtonWidget.builder(Text.of("Speak")) { b -> speakButtonPressed(b) }
                .position((width * 0.5).toInt() - (size.first / 2), (height * 0.6) .toInt() - (size.second / 2))
                .size(size.first, size.second)
                .build()
            this.addDrawableChild(speakButton)
        }

        // Initialization thingies
        SpeechUtil.stopSpeaking()
        setInitialFocus(textBox)
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        context!!.setShaderColor(0.25f, 0.25f, 0.25f, 1.0f)
        context.drawTexture(Identifier.of(MOD_ID, "textures/gui/speak_background"), 0, 0, 0, 0.0f, 0.0f, width, height, width, height)
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

        super.render(context, mouseX, mouseY, delta)
    }

    fun speakButtonPressed(button: ButtonWidget?) {
        println(textBox.text)
        SpeechUtil.speak(textBox.text)
        MinecraftClient.getInstance().setScreen(null);
    }
}
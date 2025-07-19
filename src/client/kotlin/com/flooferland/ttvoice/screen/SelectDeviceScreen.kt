package com.flooferland.ttvoice.screen

import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.util.math.Vector2Int
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.CyclingButtonWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import javax.sound.sampled.AudioSystem

// TODO: Fix bug where resizing the window makes the several button layout no longer show the selected device

class SelectDeviceScreen(val parent: Screen) : Screen(Text.of("Audio device select screen")) {
    val buttons = mutableListOf<ButtonWidget>()
    lateinit var singleButton: CyclingButtonWidget<Int>

    fun getButtonSize(): Vector2Int {
        return Vector2Int((width * 0.95).toInt(), 15)
    }

    override fun init() {
        val buttonSize = getButtonSize()

        // Back button
        run {
            val pad = 10
            val size = Vector2Int(50, 20)
            val backButton = ButtonWidget.builder(Text.of("Back"))
                { MinecraftClient.getInstance().setScreen(parent) }
                .position(pad, height - ((pad*2) + (size.y / 2)))
                .size(size.x, size.y)
                .build()
            addDrawableChild(backButton)
        }

        // Devices
        run {
            val mixers = AudioSystem.getMixerInfo()
            val pad = 3

            // Several buttons
            for ((i, mixer) in mixers.withIndex()) {
                val row = (i % 2)
                val size = Vector2Int(buttonSize.x / 2, buttonSize.y)
                val position =
                    if (row == 0)
                        Vector2Int((width / 2) - (buttonSize.x / 2) - pad, pad + (i * (size.y / 2)))
                    else
                        Vector2Int(pad + (width / 2), pad + ((i-1) * (size.y / 2)))
                val button = ButtonWidget.builder(Text.of(mixer.name))
                    { b ->
                        ModState.config.audio.device = i
                        updateButtonStyles()
                    }
                    .position(position.x, position.y)
                    .size(size.x, size.y)
                    .build()
                addDrawableChild(button)
                buttons.add(button)
            }

            // Backup in case the screen is too small
            singleButton = CyclingButtonWidget.builder<Int>()
                { v -> Text.of(mixers.get(ModState.config.audio.device).name) }
                .values((mixers.size downTo 0).toList())
                .build(
                    (width / 2)  - (buttonSize.x / 2), height / 2,
                    buttonSize.x, buttonSize.y,
                    Text.of("Audio output device")
                ) { b, v -> ModState.config.audio.device = v }
            addDrawableChild(singleButton)
        }

        // Updating
        updateVisibility()
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(context)
        super.render(context, mouseX, mouseY, delta)
    }

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        super.resize(client, width, height)
        updateVisibility()
    }

    fun updateVisibility() {
        val mixers = AudioSystem.getMixerInfo()
        val screenTooSmall = ((getButtonSize().y * 0.55) * mixers.size) > height

        singleButton.visible = screenTooSmall
        for (button in buttons) {
            button.visible = !screenTooSmall
        }
        updateButtonStyles()
    }

    fun updateButtonStyles() {
        for ((i, button) in buttons.withIndex()) {
            val selected = (ModState.config.audio.device == i)
            button.message = MutableText.of(button.message.content)
                .setStyle(
                    Style.EMPTY
                        .withBold(selected)
                        .withUnderline(selected)
                )
        }
    }
}
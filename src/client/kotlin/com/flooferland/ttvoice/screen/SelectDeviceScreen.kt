package com.flooferland.ttvoice.screen

import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.registry.ModConfig
import com.flooferland.ttvoice.speech.SpeechUtil
import com.flooferland.ttvoice.util.math.Vector2Int
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.CyclingButtonWidget
import net.minecraft.client.gui.widget.TextWidget
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

    init {
        // Speaker init
        if (SpeechUtil.isNotInitialized()) {
            SpeechUtil.load(null)
        }
    }

    override fun init() {
        val buttonSize = getButtonSize()

        // Back button
        val backButtonSize = Vector2Int(50, 20)
        val bottomHeight = (height - (20 + (backButtonSize.y / 2)))
        val backButton = ButtonWidget.builder(Text.of("Back"))
            { MinecraftClient.getInstance().setScreen(parent) }
            .position(10, bottomHeight)
            .size(backButtonSize.x, backButtonSize.y)
            .build()
        addDrawableChild(backButton)

        // Temporary info label
        run {
            val warnLabel = TextWidget(
                20 + backButtonSize.x, bottomHeight,
                (width * 0.6).toInt(), 20,
                Text.of("**Test audio might take up to a second to play"), textRenderer
            ).alignLeft()
            addDrawableChild(warnLabel)
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
                        ModConfig.save()
                        updateButtonStyles()

                        SpeechUtil.shutUp()
                        SpeechUtil.playTest()
                    }
                    .position(position.x, position.y)
                    .size(size.x, size.y)
                    .build()
                addDrawableChild(button)
                buttons.add(button)
            }

            // Backup in case the screen is too small
            singleButton = CyclingButtonWidget.builder<Int>()
                { v -> Text.of(mixers.getOrNull(ModState.config.audio.device)?.name ?: "Unknown") }
                .values((mixers.size downTo 0).toList())
                .build(
                    (width / 2)  - (buttonSize.x / 2), height / 2,
                    buttonSize.x, buttonSize.y,
                    Text.of("Audio output device")
                ) { b, v ->
                    ModState.config.audio.device = v
                    ModConfig.save()

                    SpeechUtil.shutUp()
                    SpeechUtil.playTest()
                }
            addDrawableChild(singleButton)
        }

        // Updating
        updateVisibility()
    }

    override fun removed() {
        // Un-init speaker if not in a world
        if (MinecraftClient.getInstance().world == null && SpeechUtil.isInitialized()) {
            SpeechUtil.unload()
        }
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        //? if >=1.21 {
        renderBackground(context, mouseX, mouseY, delta)
        //?} else {
        /*renderBackground(context)
        *///?}
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
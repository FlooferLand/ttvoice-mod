package com.flooferland.ttvoice.screen

import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.registry.ModConfig
import com.flooferland.ttvoice.speech.SpeechUtil
import com.flooferland.ttvoice.util.math.Vector2Int
import net.minecraft.client.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.network.chat.*
import javax.sound.sampled.AudioSystem

// TODO: Fix bug where resizing the window makes the several button layout no longer show the selected device

class SelectDeviceScreen(val parent: Screen) : Screen(Component.literal("Audio device select screen")) {
    val buttons = mutableListOf<Button>()
    lateinit var singleButton: CycleButton<Int>

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
        val backButton = Button.builder(Component.literal("Back"))
            { Minecraft.getInstance().setScreen(parent) }
            .pos(10, bottomHeight)
            .size(backButtonSize.x, backButtonSize.y)
            .build()
        addRenderableWidget(backButton)

        // Temporary info label
        run {
            val warnLabel = StringWidget(
                20 + backButtonSize.x, bottomHeight,
                (width * 0.6).toInt(), 20,
                Component.literal("**Test audio might take up to a second to play"), font
            )
            addRenderableWidget(warnLabel)
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
                val button = Button.builder(Component.literal(mixer.name))
                    { b ->
                        ModState.config.audio.device = i
                        ModConfig.save()
                        updateButtonStyles()

                        SpeechUtil.shutUp()
                        SpeechUtil.playTest()
                    }
                    .pos(position.x, position.y)
                    .size(size.x, size.y)
                    .build()
                addRenderableWidget(button)
                buttons.add(button)
            }

            // Backup in case the screen is too small
            singleButton = CycleButton.builder<Int>()
                { v -> Component.literal(mixers.getOrNull(ModState.config.audio.device)?.name ?: "Unknown") }
                .withValues((mixers.size downTo 0).toList())
                .create(
                    (width / 2)  - (buttonSize.x / 2), height / 2,
                    buttonSize.x, buttonSize.y,
                    Component.literal("Audio output device")
                ) { b, v ->
                    ModState.config.audio.device = v
                    ModConfig.save()

                    SpeechUtil.shutUp()
                    SpeechUtil.playTest()
                }
            addRenderableWidget(singleButton)
        }

        // Updating
        updateVisibility()
    }

    override fun removed() {
        // Un-init speaker if not in a level
        if (Minecraft.getInstance().level == null && SpeechUtil.isInitialized()) {
            SpeechUtil.unload()
        }
    }

    override fun resize(client: Minecraft, width: Int, height: Int) {
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
            button.message = Component.literal(button.message.string)
                .setStyle(
                    Style.EMPTY
                        .withBold(selected)
                        .withUnderlined(selected)
                )
        }
    }
}
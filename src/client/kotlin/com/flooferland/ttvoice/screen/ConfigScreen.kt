package com.flooferland.ttvoice.screen

import com.flooferland.espeak.Espeak
import com.flooferland.ttvoice.TextToVoiceClient.Companion.LOGGER
import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.data.TextToVoiceConfig
import com.flooferland.ttvoice.data.TextToVoiceConfig.*
import com.flooferland.ttvoice.registry.ModConfig
import com.flooferland.ttvoice.speech.SpeechThread
import com.flooferland.ttvoice.speech.SpeechUtil
import com.flooferland.ttvoice.util.ColorUtils
import com.flooferland.ttvoice.util.Extensions.compatHoverTooltip
import com.flooferland.ttvoice.util.SatisfyingNoises
import com.flooferland.ttvoice.util.Utils
import com.flooferland.ttvoice.util.math.MutVector2Int
import com.flooferland.ttvoice.util.math.Vector2Int
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val title = Component.translatable("config.${MOD_ID}.title")
private val WHITE_COLOR: Int = ColorUtils.getColor(255, 255, 255)

typealias TabId = String

class ConfigScreen(val parent: Screen) : Screen(title) {
    val widgets = mutableMapOf<TabId, MutableList<AbstractWidget>>()
    val donationWidgets = mutableListOf<AbstractButton>()
    var categoryContext: TabId = ""
    var selectedCategory: TabId = ""
    lateinit var saveButton: Button
    lateinit var noticeLabel: StringWidget
    lateinit var currentConfig: TextToVoiceConfig
    lateinit var selectDeviceButton: Button
    lateinit var testButton: Button
    var error: Error? = null
        get() = field
        set(value) {
            field = value
            donationWidgets.forEach { it.visible = (value == null) }
            if (value != null) {
                noticeLabel.message = Component.literal(value.message.orEmpty())
                    .setStyle(Style.EMPTY.applyFormats(ChatFormatting.RED, ChatFormatting.BOLD))
                noticeLabel.visible = true
                saveButton.active = false
            } else {
                noticeLabel.visible = false
                saveButton.active = true
                warning = warning
            }
        }
    var warning: Error? = null
        get() = field
        set(value) {
            field = value
            donationWidgets.forEach { it.visible = (value == null) }
            if (error == null) {
                if (value != null) {
                    noticeLabel.message = Component.literal(value.message)
                        .setStyle(Style.EMPTY.applyFormats(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                    noticeLabel.visible = true
                } else {
                    noticeLabel.visible = false
                }
            }
        }
    var dirty: Boolean = false
        get() = field
        set(value) {
            field = value
            if (value /* Dirty */ && error == null) {
                saveButton.active = true
            } else if (error == null) {
                saveButton.active = false
            }
        }

    override fun init() {
        currentConfig = ModState.config.clone()

        // Speaker init
        if (SpeechUtil.isNotInitialized()) {
            SpeechUtil.load(null)
        }

        run {
            val pad = Vector2Int(10, 5)
            val size = Vector2Int(60, 20)

            // Back button
            run {
                val backButton = Button.builder(Component.literal("Back"))
                { Minecraft.getInstance().setScreen(parent) }
                    .pos(pad.x, height - size.y - pad.y)
                    .size(size.x, size.y)
                    .build()
                addRenderableWidget(backButton)
            }

            // Save button
            saveButton = Button.builder(Component.literal("Save"))
            { saveSettings() }
                .pos(pad.x + (size.x + pad.x), height - size.y - pad.y)
                .size(size.x, size.y)
                .build()
            saveButton.active = false
            addRenderableWidget(saveButton)

            // Audio test button
            testButton = Button.builder(Component.literal("Test"))
            { SpeechUtil.playTest() }
                .pos(pad.x + ((size.x + pad.x) * 2), height - size.y - pad.y)
                .size(size.x, size.y)
                .build()
            addRenderableWidget(testButton)

            // Donate button
            run {
                val size = Vector2Int((size.x * 1.7).toInt(), size.y)
                val pos = Vector2Int(width - size.x - pad.x, height - size.y - pad.y)

                // Tiny buttons
                val kofiButton = Button.builder(Component.literal("Ko-Fi"))
                { Utils.openLink(this, "https://ko-fi.com/FlooferLand") }
                    .pos(pos.x, pos.y)
                    .size(size.x / 2, size.y)
                    .build()
                addRenderableWidget(kofiButton)
                donationWidgets.add(kofiButton)
                kofiButton.visible = false

                val patreonButton = Button.builder(Component.literal("Patreon"))
                { Utils.openLink(this, "https://patreon.com/FlooferLand") }
                    .pos(pos.x + (size.x / 2), pos.y)
                    .size(size.x / 2, size.y)
                    .build()
                addRenderableWidget(patreonButton)
                donationWidgets.add(patreonButton)
                patreonButton.visible = false

                // Main donate button
                val donateButton = Button.builder(Component.literal("♡ Support me"))
                    { b ->
                        b.visible = false
                        kofiButton.visible = true
                        patreonButton.visible = true
                    }
                    .pos(pos.x, pos.y)
                    .size(size.x, size.y)
                    .build()
                donateButton.setAlpha(0.5f)
                addRenderableWidget(donateButton)
                donationWidgets.add(donateButton)
            }

            // Error / warning
            noticeLabel = StringWidget(
                pad.x, height - (size.y * 2) - pad.y,
                500, 20,
                Component.literal(""), font
            )
            addRenderableWidget(noticeLabel)
        }

        // Auto-settings
        autoSettings()
    }

    override fun removed() {
        // Un-init speaker if not in a world
        if (Minecraft.getInstance().level == null && SpeechUtil.isInitialized()) {
            SpeechUtil.unload()
        }
    }

    fun autoSettings() {
        for (array in widgets.values) {
            for (widget in array) {
                removeWidget(widget)
            }
        }
        widgets.clear()
        selectedCategory = TextToVoiceConfig::class.memberProperties.first().name

        for (categoryProp in TextToVoiceConfig::class.memberProperties) {
            val categoryName = categoryProp.name
            var categoryValue = (categoryProp.get(currentConfig) ?: continue)
            categoryContext = categoryName
            widgets[categoryContext] = mutableListOf()

            // Special redirect for voice config, since the type changes
            if (categoryValue is VoiceConfig) {
                when (ModState.config.audio.ttsBackend) {
                    TTSBackend.Espeak -> categoryValue = categoryValue.espeak
                }
            }

            // Loop and populate all the props
            val offset = MutVector2Int(10, 40 /* Space for the title */)
            for ((i, field) in categoryValue::class.memberProperties.withIndex()) {
                val fieldName = field.name
                val fieldInitialValue = (field as KProperty1<Any, *>).get(categoryValue)

                val position = Vector2Int(offset.x, offset.y)
                var size = Vector2Int(0, 0)

                val translationString = "config.${MOD_ID}.${categoryName}.${fieldName}"
                val labelText = Component.translatable(translationString);
                val experimentalLabelText = labelText.copy().withStyle(ChatFormatting.YELLOW)

                run {
                    // Manually displayed
                    when (fieldName) {
                        // Select audio device button
                        AudioConfig::device.name -> {
                            size = Vector2Int(300, 18)
                            selectDeviceButton = Button.Builder(experimentalLabelText)
                                {
                                    saveSettings()
                                    Minecraft.getInstance().setScreen(SelectDeviceScreen(this))
                                }
                                .tooltip(Tooltip.create(Component.literal("Any TTS audio can be additionally routed through another device, for example your 2nd pair of speakers (or Voicemeeter)\n\nNOTE: This is experimental!\nYou may get an error when selecting a device.").withStyle(ChatFormatting.YELLOW)))
                                .pos(position.x, position.y)
                                .size(size.x, size.y)
                                .build()
                            selectDeviceButton.active = ModState.config.general.routeThroughDevice
                            addConfigWidget(selectDeviceButton)
                            return@run
                        }

                        // TTS backend cycling button
                        AudioConfig::ttsBackend.name -> {
                            size = Vector2Int(300, 18)
                            val b = CycleButton.Builder<TextToVoiceConfig.TTSBackend>()
                                { v -> Component.literal(v::name.get()) }
                                .withTooltip { t -> Tooltip.create(Component.literal("The backend TTS system")) }
                                .withValues(TextToVoiceConfig.TTSBackend::entries.get())
                                .withInitialValue(fieldInitialValue as TextToVoiceConfig.TTSBackend)
                                .create(position.x, position.y, size.x, size.y, labelText)
                                { b, v ->
                                    /*if (v == TextToVoiceConfig.TTSBackend.Lua) {
                                        warning = Error("Lua backend requires custom configuration. See the mod page")
                                    } else {
                                        warning = null
                                    }*/
                                    setSetting(field, categoryValue, v)
                                }
                            addConfigWidget(b)
                            return@run
                        }

                        // --------------------
                        // -   Voice config   -
                        // --------------------

                        // Voices picker cycling button
                        VoiceConfig::espeak::name.name -> {
                            size = Vector2Int(300, 18)
                            val voices = SpeechUtil.getVoices()
                            var initialVoice: Espeak.Voice? = null
                            for (voice in voices) {
                                if (((fieldInitialValue as String?) ?: SpeechThread.DEFAULT_VOICE) == voice.identifier) {
                                    initialVoice = voice
                                    break
                                }
                            }
                            if (initialVoice == null) {
                                initialVoice = voices.firstOrNull()
                            }
                            if (initialVoice == null) {
                                error = Error("Native call failed. Make sure you're using the right Java version (initialVoice == null)")
                                break;
                            }
                            val b = CycleButton.Builder<Espeak.Voice>()
                                { v -> Component.literal(v.name) }
                                .withTooltip { t -> Tooltip.create(Component.literal("The TTS voice preset.\n\nShift-click to scroll back.")) }
                                .withValues(voices)
                                .withInitialValue(initialVoice)
                                .create(position.x, position.y, size.x, size.y, labelText)
                                { b, v ->
                                    currentConfig.voice.espeak.name = v.identifier
                                    updateMarkDirty()
                                    SpeechUtil.updateVoice(currentConfig.voice.espeak.name)
                                    SpeechUtil.playTest()
                                }
                            addConfigWidget(b)
                            return@run
                        }
                    }

                    // Automatically displayed values
                    val type = field.returnType;
                    when (type.classifier) {
                        Boolean::class -> {
                            size = Vector2Int(300, 20)
                            fun onClick() {
                                val on = !(field.get(categoryValue) as Boolean)
                                setSetting(field, categoryValue, on)
                                if (fieldName == GeneralConfig::routeThroughDevice.name) {
                                    selectDeviceButton.active = on
                                }
                            }

                            //? if >1.20.1 {
                            /*val thing = Checkbox.builder(labelText, font)
                                .selected(fieldInitialValue as Boolean)
                                .pos(position.x, position.y)
                                .onValueChange { _, _ -> onClick() }
                                .build()
                            *///?} else {
                            val thing = object : Checkbox(position.x, position.y, size.x, size.y, labelText, fieldInitialValue as Boolean) {
                                override fun onClick(mouseX: Double, mouseY: Double) {
                                    super.onClick(mouseX, mouseY)
                                    onClick();
                                }
                            }
                            //?}
                            addConfigWidget(thing)
                        }

                        String::class -> {
                            size = Vector2Int(300, 35)

                            val label = StringWidget(offset.x + 5, offset.y, 200, 15, labelText, font)
                            addRenderableWidget(label)
                            addConfigWidget(label)

                            val tooltip = Component.translatableWithFallback("${translationString}.tooltip", labelText.string)
                            val thing = EditBox(font, position.x+1, position.y+15, size.x, 18, Component.literal(fieldInitialValue as String))
                            thing.setTooltip(Tooltip.create(tooltip))
                            thing.setMaxLength(500)
                            thing.value = fieldInitialValue
                            thing.setResponder { v ->
                                setSetting(field, categoryValue, v)
                            }
                            addConfigWidget(thing)
                        }

                        else -> {
                            println("Unknown type in config ${categoryName}.${fieldName} (${type.classifier})")
                        }
                    }
                }

                val pad = 5;
                offset.y += (size.y + pad)
            }
        }

        // Only showing widgets of the current category
        for ((id, widgets) in widgets) {
            for (widget in widgets) {
                widget.visible = (id == selectedCategory)
            }
        }

        // Add category tab buttons
        val xPad = 8
        var xOffset = 120 // Offset for the title
        val targetButtonSize = Vector2Int(80, 18)
        val minWidth = xOffset + (targetButtonSize.x + xPad) * widgets.keys.size
        var size = targetButtonSize
        if (width < minWidth) {
            size = Vector2Int(targetButtonSize.x - ((minWidth - width) * 0.25).toInt(), targetButtonSize.y)
        }
        val tabButtons = mutableListOf<AbstractButton>()
        for (id in widgets.keys) {
            val button = Button.builder(Component.translatable("config.$MOD_ID.$id"))
                { b ->
                    selectedCategory = id

                    for (button in tabButtons) {
                        button.setAlpha(if (button == b) 1.0f else 0.6f)
                    }

                    for ((id, widgets) in widgets) {
                        for (widget in widgets) {
                            widget.visible = (id == selectedCategory)
                        }
                    }
                }
                .pos(xOffset, 5)
                .size(size.x, size.y)
                .build()
            button.setAlpha(if (id == selectedCategory) 1.0f else 0.6f)
            addRenderableWidget(button)
            tabButtons.add(button)
            xOffset += size.x + xPad
        }
    }

    fun addConfigWidget(widget: AbstractWidget) {
        widgets[categoryContext]?.add(widget)
        addRenderableWidget(widget)
    }

    fun <T> setSetting(field: KProperty1<Any, T>, categoryValue: Any, value: T) {
        (field as KMutableProperty1<Any, T>).set(categoryValue, value)
        updateMarkDirty()
    }

    fun updateMarkDirty() {
        dirty = (!currentConfig.compare(ModState.config))
    }

    fun saveSettings() {
        ModState.config = currentConfig.clone()
        ModConfig.save()
        SatisfyingNoises.playSuccess()
        saveButton.active = false
    }

    override fun resize(client: Minecraft, width: Int, height: Int) {
        super.resize(client, width, height)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        fun drawHeaderGradient() =
            context.fillGradient(
                0, 0, width, 30,
                ColorUtils.getColor(0, 0, 0, 255),
                ColorUtils.getColor(0, 0, 0, 0)
            )

        //? if >1.21 {
        /*drawHeaderGradient()
        super.render(context, mouseX, mouseY, delta)
        *///?} else if >=1.20.4 {
        /*super.renderBackground(context, mouseX, mouseY, delta)
        drawHeaderGradient()
        super.render(context, mouseX, mouseY, delta)
        *///?} else {
        super.renderBackground(context)
        drawHeaderGradient()
        super.render(context, mouseX, mouseY, delta)
        //?}

        context.drawCenteredString(font, com.flooferland.ttvoice.screen.title, 10, 10, WHITE_COLOR)
    }
}
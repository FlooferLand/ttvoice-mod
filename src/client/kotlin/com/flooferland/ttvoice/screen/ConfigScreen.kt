package com.flooferland.ttvoice.screen

import com.flooferland.espeak.Espeak
import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.data.TextToVoiceConfig
import com.flooferland.ttvoice.data.TextToVoiceConfig.*
import com.flooferland.ttvoice.registry.ModConfig
import com.flooferland.ttvoice.speech.SpeechUtil
import com.flooferland.ttvoice.util.ColorUtils
import com.flooferland.ttvoice.util.SatisfyingNoises
import com.flooferland.ttvoice.util.math.MutVector2Int
import com.flooferland.ttvoice.util.math.Vector2Int
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.*
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val title = Text.translatable("config.${MOD_ID}.title")
private val WHITE_COLOR: Int = ColorUtils.getColor(255, 255, 255, 255)

typealias TabId = String

class ConfigScreen(val parent: Screen) : Screen(title) {
    val widgets = mutableMapOf<TabId, MutableList<ClickableWidget>>()
    var categoryContext: TabId = ""
    var selectedCategory: TabId = ""
    lateinit var saveButton: ButtonWidget
    lateinit var noticeLabel: TextWidget
    lateinit var currentConfig: TextToVoiceConfig
    lateinit var selectDeviceButton: ButtonWidget
    lateinit var testButton: ButtonWidget
    var error: Error? = null
        get() = field
        set(value) {
            field = value
            if (value != null) {
                noticeLabel.message = Text.literal(value.message)
                    .setStyle(Style.EMPTY.withFormatting(Formatting.RED, Formatting.BOLD))
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
            if (error == null) {
                if (value != null) {
                    noticeLabel.message = Text.literal(value.message)
                        .setStyle(Style.EMPTY.withFormatting(Formatting.YELLOW, Formatting.BOLD))
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
            // Back button
            val pad = Vector2Int(10, 5)
            val size = Vector2Int(60, 25)
            val backButton = ButtonWidget.builder(Text.of("Back"))
                { MinecraftClient.getInstance().setScreen(parent) }
                .position(pad.x, height - size.y - pad.y)
                .size(size.x, size.y)
                .build()
            addDrawableChild(backButton)

            // Save button
            saveButton = ButtonWidget.builder(Text.of("Save"))
            { saveSettings() }
                .position(pad.x + (size.x + pad.x), height - size.y - pad.y)
                .size(size.x, size.y)
                .build()
            saveButton.active = false
            addDrawableChild(saveButton)

            // Audio test button
            testButton = ButtonWidget.builder(Text.of("Test"))
            { SpeechUtil.playTest() }
                .position(pad.x + ((size.x + pad.x) * 2), height - size.y - pad.y)
                .size(size.x, size.y)
                .build()
            addDrawableChild(testButton)

            // Error / warning
            noticeLabel = TextWidget(
                pad.x, height - (size.y * 2) - pad.y,
                500, 20,
                Text.of(""), textRenderer
            )
            addDrawableChild(noticeLabel)
        }

        // Auto-settings
        autoSettings()
    }

    override fun removed() {
        // Un-init speaker if not in a world
        if (MinecraftClient.getInstance().world == null && SpeechUtil.isInitialized()) {
            SpeechUtil.unload()
        }
    }

    fun autoSettings() {
        for (array in widgets.values) {
            for (widget in array) {
                remove(widget)
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
                val translationString = "config.${MOD_ID}.${categoryName}.${fieldName}"
                val labelText = Text.translatable(translationString);

                val position = Vector2Int(offset.x, offset.y)
                var size = Vector2Int(0, 0)

                val experimentalLabelText = labelText.formatted(Formatting.YELLOW)
                    //? if <1.21.9 {
                    .setStyle(Style.EMPTY.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Experimental"))))
                    //?}

                run {
                    // Manually displayed
                    when (fieldName) {
                        // Select audio device button
                        AudioConfig::device.name -> {
                            size = Vector2Int(300, 18)
                            selectDeviceButton = ButtonWidget.Builder(experimentalLabelText)
                                {
                                    saveSettings()
                                    MinecraftClient.getInstance().setScreen(SelectDeviceScreen(this))
                                }
                                .position(position.x, position.y)
                                .size(size.x, size.y)
                                .build()
                            selectDeviceButton.active = ModState.config.general.routeThroughDevice
                            addConfigWidget(selectDeviceButton)
                            return@run
                        }

                        // TTS backend cycling button
                        AudioConfig::ttsBackend.name -> {
                            size = Vector2Int(300, 18)
                            val b = CyclingButtonWidget.Builder<TextToVoiceConfig.TTSBackend>()
                                { v -> Text.of(v::name.get()) }
                                .values(TextToVoiceConfig.TTSBackend::entries.get())
                                .initially(fieldInitialValue as TextToVoiceConfig.TTSBackend)
                                .build(position.x, position.y, size.x, size.y, labelText)
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
                            var initialVoice = Optional.empty<Espeak.Voice>()
                            for (voice in voices) {
                                if ((fieldInitialValue as String?).let { voice.identifier == it || it == null }) {
                                    initialVoice = Optional.of<Espeak.Voice>(voice)
                                    break
                                }
                            }
                            val b = CyclingButtonWidget.Builder<Optional<Espeak.Voice>>()
                            { v -> Text.of(v.getOrNull()?.name ?: "Default") }
                                .values(voices.map { Optional.of(it) }.plusElement(Optional.empty<Espeak.Voice>()))
                                .initially(initialVoice)
                                .build(position.x, position.y, size.x, size.y, labelText)
                                { b, v ->
                                    currentConfig.voice.espeak.name = v.getOrNull()?.identifier
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
                            /*val thing = CheckboxWidget.builder(labelText, textRenderer)
                                .checked(fieldInitialValue as Boolean)
                                .pos(position.x, position.y)
                                .callback { _, _ ->
                                    onClick()
                                }
                                .build()
                            *///?} else {
                            val thing = object : CheckboxWidget(position.x, position.y, size.x, size.y, labelText, fieldInitialValue as Boolean) {
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

                            val label = TextWidget(offset.x + 5, offset.y, 200, 15, labelText, textRenderer)
                                .setTextColor(WHITE_COLOR)
                            addDrawableChild(label)
                            addConfigWidget(label)

                            val tooltip = Text.translatableWithFallback("${translationString}.tooltip", labelText.string)
                            val thing = TextFieldWidget(textRenderer, position.x+1, position.y+15, size.x, 18, Text.of(fieldInitialValue as String))
                            thing.setTooltip(Tooltip.of(tooltip))
                            thing.setMaxLength(500)
                            thing.text = fieldInitialValue
                            thing.setChangedListener { v ->
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
        val tabButtons = mutableListOf<ClickableWidget>()
        for (id in widgets.keys) {
            val button = ButtonWidget.builder(Text.translatable("config.$MOD_ID.$id"))
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
                .position(xOffset, 5)
                .size(size.x, size.y)
                .build()
            button.setAlpha(if (id == selectedCategory) 1.0f else 0.6f)
            addDrawableChild(button)
            tabButtons.add(button)
            xOffset += size.x + xPad
        }
    }

    fun addConfigWidget(widget: ClickableWidget) {
        widgets[categoryContext]?.add(widget)
        addDrawableChild(widget)
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

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        super.resize(client, width, height)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        fun drawHeaderGradient() =
            context.fillGradient(
                0, 0, width, 30,
                ColorUtils.getColor(0, 0, 0, 255),
                ColorUtils.getColor(0, 0, 0, 0)
            )

        //? if >1.21 {
        /*drawHeaderGradient()
        super.render(context, mouseX, mouseY, delta)
        *///?} else {
        super.renderBackground(context)
        drawHeaderGradient()
        super.render(context, mouseX, mouseY, delta)
        //?}

        context.drawTextWithShadow(textRenderer, com.flooferland.ttvoice.screen.title, 10, 10, WHITE_COLOR)
    }
}
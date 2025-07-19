package com.flooferland.ttvoice.screen

import com.flooferland.ttvoice.TextToVoiceClient
import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.data.TextToVoiceConfig
import com.flooferland.ttvoice.data.TextToVoiceConfig.AudioConfig
import com.flooferland.ttvoice.registry.ModConfig
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.*
import net.minecraft.text.Text
import net.minecraft.util.math.ColorHelper
import org.joml.Vector2i
import javax.sound.sampled.AudioSystem
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

private val title = Text.translatable("config.${MOD_ID}.title")
private val WHITE_COLOR: Int = ColorHelper.Argb.getArgb(255, 255, 255, 255)

class ConfigScreen(val parent: Screen) : Screen(title) {

    override fun init() {
        // Back button
        run {
            val pad = Pair(10, 5)
            val size = Pair(40, 20)
            val backButton = ButtonWidget.builder(Text.of("Back"), { MinecraftClient.getInstance().setScreen(parent) })
                .position(pad.first, height - size.second - pad.second)
                .size(size.first, size.second)
                .build()
            addDrawableChild(backButton)
        }

        // Auto-settings
        for (categoryProp in TextToVoiceConfig::class.memberProperties) {
            val categoryName = categoryProp.name
            val categoryValue = categoryProp.get(ModState.config) ?: continue

            for ((i, field) in categoryValue::class.memberProperties.withIndex()) {
                val fieldName = field.name
                val fieldValue = (field as KProperty1<Any, *>).get(categoryValue) ?: continue

                // Manually displayed
                when (fieldName) {
                    AudioConfig::device.name -> {
                        // Audio devices button
                        run {
                            val labelText = Text.translatable("config.${MOD_ID}.field.audio.device")
                            val size = Pair(300, 20)
                            val b = ButtonWidget.Builder(Text.of("Open device picker"))
                            { MinecraftClient.getInstance().setScreen(SelectDeviceScreen(this)) }
                                .position(
                                    (parent.width * 0.5).toInt() - (size.first / 2),
                                    (parent.height * 0.5).toInt() - (size.second / 2),
                                )
                                .size(size.first, size.second)
                                .build()
                            val label = TextWidget(20, b.y, parent.width / 2, 20, labelText, textRenderer).alignLeft()
                            addDrawableChild(b)
                            addDrawableChild(label)
                        }
                        return
                    }
                }

                // Automatically displayed
                val position = Vector2i(
                    width / 2, i * 30
                )
                val type = field.returnType;
                val translationString = Text.translatable("config.${MOD_ID}.field.${categoryName}.${fieldName}");
                @Suppress("UNCHECKED_CAST")
                when (type.classifier) {
                    Boolean::class -> {
                        val thing = object : CheckboxWidget(position.x, position.y, 0, 0, translationString, fieldValue as Boolean) {
                            override fun onClick(mouseX: Double, mouseY: Double) {
                                super.onClick(mouseX, mouseY)
                                (field as KMutableProperty1<Any, Boolean>).set(categoryValue, !(fieldValue as Boolean))
                            }
                        }
                        addDrawableChild(thing)
                    }
                    String::class -> {
                        val thing = TextFieldWidget(textRenderer, position.x, position.y, 0, 0, translationString)
                        thing.setChangedListener { v ->
                            (field as KMutableProperty1<Any, String>).set(categoryValue, v)
                        }
                        addDrawableChild(thing)
                    }
                    else -> {
                        println("Unknown type in config ${categoryName}.${fieldName} (${type.classifier})")
                    }
                }
            }
        }
        recalculateDimensions()
    }

    fun recalculateDimensions() {

    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        super.resize(client, width, height)
        recalculateDimensions()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawCenteredTextWithShadow(textRenderer, com.flooferland.ttvoice.screen.title, width / 2, 15, WHITE_COLOR)
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)
    }
}
package com.flooferland.ttvoice.screen

import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.*
import net.minecraft.text.Text
import net.minecraft.util.math.ColorHelper
import javax.sound.sampled.AudioSystem

private val title = Text.translatable("config.${MOD_ID}.title")
private val WHITE_COLOR: Int = ColorHelper.Argb.getArgb(255, 255, 255, 255)

class ConfigScreen(val parent: Screen) : Screen(title) {
    private lateinit var scrollContainer: ConfigScreenScrollable

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

        // Config content
        scrollContainer = ConfigScreenScrollable(
            this,
            MinecraftClient.getInstance(),
            width, height,
            30, height - 30,
            200
        )
        addDrawableChild(scrollContainer)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawCenteredTextWithShadow(textRenderer, com.flooferland.ttvoice.screen.title, width / 2, 15, WHITE_COLOR)
        scrollContainer.render(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
    }

    // Scrollable widget
    private class ConfigScreenScrollable : ElementListWidget<ConfigScreenScrollable.Entry> {
        val screen: ConfigScreen
        constructor(screen: ConfigScreen, client: MinecraftClient, width: Int, height: Int, top: Int, bottom: Int, itemHeight: Int)
            : super(client, width, height, top, bottom, itemHeight) {
                this.screen = screen
            }

        override fun enableScissor(context: DrawContext) {
            context.enableScissor(this.left, this.top + 4, this.right, this.bottom)
        }

        override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
            super.render(context, mouseX, mouseY, delta)
            //screen.render(context, mouseX, mouseY, delta)
        }

        private class Entry(val screen: ConfigScreen, val parent: ConfigScreenScrollable) : ElementListWidget.Entry<ConfigScreenScrollable.Entry>() {
            val widgets: ArrayList<ClickableWidget> = arrayListOf()

            init {
                // Audio devices button
                run {
                    val labelText = Text.translatable("config.${MOD_ID}.field.audio.device")
                    val size = Pair(300, 20)
                    val b = CyclingButtonWidget.Builder<Int>({ v ->
                        return@Builder Text.of(AudioSystem.getMixerInfo().get(v).name)
                    })
                        .values((AudioSystem.getMixerInfo().lastIndex downTo 0).toList())
                        .build(
                            (parent.width * 0.5).toInt() - (size.first / 2),
                            (parent.height * 0.5).toInt() - (size.second / 2),
                            size.first, size.second,
                            labelText
                        )
                    val label = TextWidget(20, b.y, parent.width / 2, 20, labelText, screen.textRenderer).alignLeft()
                    widgets.add(label)
                    widgets.add(b)
                }
            }

            override fun render(
                context: DrawContext,
                index: Int,
                y: Int,
                x: Int,
                entryWidth: Int,
                entryHeight: Int,
                mouseX: Int,
                mouseY: Int,
                hovered: Boolean,
                delta: Float
            ) {
                for (widget in widgets) {
                    widget.render(context, mouseX, mouseY, delta)
                }
            }

            override fun children(): List<Element> {
                return widgets
            }

            override fun selectableChildren(): List<Selectable> {
                return widgets
            }
        }
    }
}
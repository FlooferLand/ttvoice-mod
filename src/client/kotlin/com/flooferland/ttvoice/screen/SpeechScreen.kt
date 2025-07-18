package com.flooferland.ttvoice.screen

import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.screen.widgets.SpeechTextInputWidget
import com.flooferland.ttvoice.speech.SpeechUtil
import com.flooferland.ttvoice.util.SatisfyingNoises
import com.flooferland.ttvoice.util.math.Vector2Int
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.*
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.joml.Vector2i

// TODO: Fix history being displayed / traveled through backwards

// TODO: Make old history items get deleted after a while

// TODO: Add an error label

// TODO: Move history to entry list system, and make history elements clickable

class SpeechScreen() : Screen(Text.of("Speech screen")) {
    private lateinit var textBox: SpeechTextInputWidget
    private lateinit var speakButton: ButtonWidget
    private lateinit var stopButton: ButtonWidget
    private lateinit var historyToggleButton: ButtonWidget
    //private lateinit var historyWidget: HistoryWidget
    private lateinit var historyTextWidget: MultilineTextWidget
    var historyPointer = 0

    override fun init() {
        val edgePad = Vector2i((width * 0.05).toInt(), (height * 0.05).toInt())

        // Adding the voice text input textbox
        run {
            val size = Vector2Int((width * 0.95).toInt(), (height * 0.13).toInt())
            textBox = SpeechTextInputWidget(
                this,
                textRenderer,
                edgePad.x,
                (height - (size.y / 2)) - edgePad.y,
                size.x - edgePad.x,
                size.y - edgePad.y,
                Text.of("Input text here"),
                { speakActionTriggered(); }
            )
            this.addDrawableChild(textBox)
        }

        // Button row
        run {
            val baseSize = Vector2Int(70, 30)
            val basePosition = Vector2Int(0, (height * 0.83).toInt() - (baseSize.y / 2))
            val widgets = ArrayList<ClickableWidget>()

            // Speak button
            run {
                speakButton = ButtonWidget.builder(Text.of("Speak"))
                    { b -> speakActionTriggered() }
                    .size(baseSize.x, baseSize.y)
                    .build()
                widgets.add(speakButton)
            }
            // Stop speaking button
            run {
                stopButton = ButtonWidget.builder(Text.of("Stop"))
                    { b -> stopActionTriggered() }
                    .size(baseSize.x, baseSize.y)
                    .build()
                widgets.add(stopButton)
            }
            // Toggle history button
            run {
                historyToggleButton = ButtonWidget.builder(Text.of("Toggle history"))
                    { b -> toggleHistoryAction() }
                    .size((baseSize.x * 1.3).toInt(), baseSize.y)
                    .build()
                widgets.add(historyToggleButton)
            }

            // Placement
            var offset = edgePad.x + basePosition.x
            for ((i, widget) in widgets.withIndex()) {
                val pad = 10
                widget.x = offset
                widget.y = basePosition.y
                offset += widget.width + pad
                addDrawableChild(widget)
            }
        }

        // History
        run {
            historyTextWidget = MultilineTextWidget(
                Text.literal("History"),
                textRenderer
            )
            this.addDrawableChild(historyTextWidget)
            updateHistoryWidget()
        }
        /*run {
            val size = Vector2Int((width * 0.5).toInt(), 200)
            historyWidget = HistoryWidget(
                this,
                MinecraftClient.getInstance(),
                size.x, size.y,
                20, size.y - 30,
                50
            )
            this.addSelectableChild(historyWidget)
        }*/

        // Initialization thingies
        setInitialFocus(textBox)
    }

    // Gets called when the history is updated
    fun updateHistoryWidget() {
        val maxRows = 15

        // Setting history text
        val text = Text.literal("")
        for ((i, history) in SpeechScreen.history.withIndex()) {
            if (i > maxRows) continue
            val style = Style.EMPTY
                .withBold(i == historyPointer)
                .withUnderline(i == historyPointer)
            text.append(
                Text.literal(history)
                    .setStyle(style)
            )
            text.append("\n")
        }
        historyTextWidget.message = text

        // Setting history size stuff
        historyTextWidget.x = (width * 0.5).toInt() - (historyTextWidget.width / 2)
        historyTextWidget.y = (height * 0.1).toInt()
        historyTextWidget.setMaxRows(maxRows)
        historyTextWidget.setCentered(true)
    }

    fun toggleHistoryAction() {
        ModState.config.ui.viewHistory = !ModState.config.ui.viewHistory
        historyTextWidget.visible = ModState.config.ui.viewHistory
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        context!!.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680)
        if (historyTextWidget.visible) {
            val pad = 16
            context.fill(
                pad,
                historyTextWidget.y - pad,
                this.width - pad,
                (height * 0.75).toInt() - pad,
                0,
                -1072689136
            )
        }
        //historyWidget.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta)
    }

    fun speakActionTriggered() {
        var text = textBox.text.trim()
        if (text.isEmpty()) {
            SatisfyingNoises.playDeny()
            return
        }
        println(text)

        // Commands
        if (text.firstOrNull() == '/') {
            var commandSucceeded = true
            when (text.removePrefix("/")) {
                RecognizedCommands.ClearHistory.command -> {
                    SpeechScreen.history.clear()
                    updateHistoryWidget()
                }
                RecognizedCommands.ToggleHistory.command -> {
                    toggleHistoryAction()
                }
                else -> {
                    SatisfyingNoises.playDeny()
                    commandSucceeded = false
                }
            }
            if (commandSucceeded) {
                SatisfyingNoises.playSuccess()
                MinecraftClient.getInstance().setScreen(SpeechScreen());
            }
            return
        }
        if (text.length >= 2 && text.substring(0..1) == "./") {
            text = text.substring(1, text.length)
        }

        // History
        val closestIndex = if (historyPointer > 0) historyPointer - 1 else 0
        if (SpeechScreen.history.isEmpty() || SpeechScreen.history[closestIndex] != text) {
            history.add(text)
            historyPointer = SpeechScreen.history.lastIndex
            updateHistoryWidget()
        }

        // Speaking
        SpeechUtil.speak(text)
        SatisfyingNoises.playConfirm()
        MinecraftClient.getInstance().setScreen(null);
    }

    fun stopActionTriggered() {
        SpeechUtil.stopSpeaking()
    }

    /// Speech history widget
    private class HistoryWidget : EntryListWidget<SpeechScreen.HistoryWidget.Entry> {
        val screen: SpeechScreen
        constructor(screen: SpeechScreen, client: MinecraftClient, width: Int, height: Int, top: Int, bottom: Int, itemHeight: Int)
                : super(client, width, height, top, bottom, itemHeight) {
            this.screen = screen
            //this.setRenderBackground(false)
        }
        private class Entry(val screen: SpeechScreen, val parent: HistoryWidget) : EntryListWidget.Entry<HistoryWidget.Entry>() {
            val widgets: ArrayList<ClickableWidget> = arrayListOf()
            init {
                for (historyElem in SpeechScreen.history) {
                    val label = TextWidget(0, 0, parent.width / 2, 20, Text.of(historyElem), screen.textRenderer).alignLeft()
                    widgets.add(label)
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
            }
        }
        override fun appendNarrations(builder: NarrationMessageBuilder?) {
        }
    }

    enum class RecognizedCommands(val command: String) {
        ClearHistory("clearhist"),
        ToggleHistory("togglehist");

        companion object {
            fun commands(): Array<String> {
                return SpeechScreen.RecognizedCommands.entries.map { e -> e.command }.toTypedArray()
            }
        }
    }

    companion object {
        val history = arrayListOf<String>()
    }
}
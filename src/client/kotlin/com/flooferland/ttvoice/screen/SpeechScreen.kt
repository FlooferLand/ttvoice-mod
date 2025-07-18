package com.flooferland.ttvoice.screen

import com.flooferland.ttvoice.screen.widgets.SpeechTextInputWidget
import com.flooferland.ttvoice.speech.SpeechUtil
import com.flooferland.ttvoice.util.SatisfyingNoises
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.*
import net.minecraft.text.Style
import net.minecraft.text.Text

// TODO: Remaster the UI to give more space in the middle of the screen;
//       Do this by moving the speech stuff to the bottom and opening up space, while making the history list toggleable

// TODO: Fix history being displayed / traveled through backwards

// TODO: Make old history items get deleted after a while

// TODO: Add an error label

// TODO: Add a stop speaking button

// TODO: Move history to entry list system, and make history elements clickable

class SpeechScreen() : Screen(Text.of("Speech screen")) {
    private lateinit var textBox: SpeechTextInputWidget
    private lateinit var speakButton: ButtonWidget
    //private lateinit var historyWidget: HistoryWidget
    private lateinit var historyTextWidget: MultilineTextWidget
    var historyPointer = 0

    override fun init() {
        // Adding the voice text input textbox
        run {
            val size = Pair((width * 0.95).toInt(), (height * 0.1).toInt())
            textBox = SpeechTextInputWidget(
                this,
                textRenderer,
                (width * 0.5).toInt() - (size.first / 2),
                (height * 0.25).toInt() - (size.second / 2),
                size.first,
                size.second,
                Text.of("Input text here"),
                { speakActionTriggered(); }
            )
            this.addDrawableChild(textBox)
        }

        // Speak button
        run {
            val size = Pair(100, 30)
            speakButton = ButtonWidget.builder(Text.of("Speak")) { b -> speakActionTriggered() }
                .position((width * 0.5).toInt() - (size.first / 2), (height * 0.4).toInt() - (size.second / 2))
                .size(size.first, size.second)
                .build()
            this.addDrawableChild(speakButton)
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
            val size = Pair((width * 0.5).toInt(), 200)
            historyWidget = HistoryWidget(
                this,
                MinecraftClient.getInstance(),
                size.first, size.second,
                20, size.second - 30,
                50
            )
            this.addSelectableChild(historyWidget)
        }*/

        // Initialization thingies
        SpeechUtil.stopSpeaking()
        setInitialFocus(textBox)
    }

    // Gets called when the history is updated
    fun updateHistoryWidget() {
        val maxRows = 16

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
        historyTextWidget.y = (height * 0.55).toInt()
        historyTextWidget.setMaxRows(maxRows)
        historyTextWidget.setCentered(true)
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context);
        run {
            val pad = 16
            context!!.fill(
                pad,
                historyTextWidget.y - pad,
                this.width - pad,
                this.height - pad,
                0,
                -1072689136
            )
        }
        //historyWidget.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta)
    }

    fun speakActionTriggered() {
        var text = textBox.text
        if (text.isBlank()) {
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
        if (text.substring(0..1) == "./") {
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
        ClearHistory("clearhist");

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
package com.flooferland.ttvoice.speech

import com.flooferland.ttvoice.TextToVoiceClient.Companion.LOGGER
import com.flooferland.ttvoice.screen.SelectDeviceScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.jetbrains.annotations.CheckReturnValue

public object SpeechUtil {
    private var thread: SpeechThread? = null

    fun load(context: ISpeaker.WorldContext?) {
        if (thread != null) return
        thread = SpeechThread(context)
        thread?.start()
    }

    fun unload() {
        thread?.send(SpeechThread.StopThreadCommand())
        thread = null
    }

    @CheckReturnValue
    fun speak(text: String) {
        thread?.send(SpeechThread.SpeakCommand(text))
        thread?.onError { err ->
            val player = MinecraftClient.getInstance().player
            player?.sendMessage(Text.literal("Text-To-Voice Error ${err.type}:\n    ${err.context}").formatted(Formatting.RED))
        }
    }


    fun shutUp() {
        thread?.send(SpeechThread.ShutUpCommand())
    }

    fun playTest() {
        if (!isTestingArmed()) {
            LOGGER.warn("playTest was called, but audio testing was not armed")
            return
        }
        thread?.send(SpeechThread.SpeakCommand("Audio test"))
    }

    fun isTestingArmed(): Boolean {
        return MinecraftClient.getInstance().currentScreen is SelectDeviceScreen
    }

    fun isSpeaking(): Boolean {
        return thread?.isSpeaking() ?: false
    }
}
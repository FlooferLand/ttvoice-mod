package com.flooferland.ttvoice.speech

import com.flooferland.espeak.Espeak
import com.flooferland.ttvoice.TextToVoiceClient.LOGGER
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.screen.ConfigScreen
import com.flooferland.ttvoice.screen.SelectDeviceScreen
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

public object SpeechUtil {
    private var thread: SpeechThread? = null

    fun isInitialized(): Boolean = (thread != null)
    fun isNotInitialized(): Boolean = (thread == null)

    fun load(context: ISpeaker.WorldContext?) {
        if (isInitialized()) return
        thread = SpeechThread(context)
        thread?.start()
    }

    fun unload() {
        thread?.send(SpeechThread.StopThreadCommand())
        thread = null
    }

    fun speak(text: String, monophonic: Boolean = false) {
        if (thread == null) {
            LOGGER.warn("speak was called, but the audio thread was not created")
            return
        }

        thread?.send(SpeechThread.SpeakCommand(text, monophonic))
        thread?.onError { err ->
            val player = Minecraft.getInstance().player
            player?.displayClientMessage(
                Component.literal("Text-To-Voice Error ${err.type}:\n    ${err.context}")
                    .withStyle(ChatFormatting.RED),
                false
            )
        }
    }

    fun shutUp() {
        thread?.send(SpeechThread.ShutUpCommand())
    }

    fun getVoices(): List<Espeak.Voice> {
        if (isNotInitialized()) {
            LOGGER.warn("${::getVoices.name} called but eSpeak was not initialized. Returning an empty array")
            return listOf()
        }
        return Espeak.listVoices()
    }

    fun playTest() {
        if (!isTestingArmed()) {
            LOGGER.warn("playTest was called, but audio testing was not armed")
            return
        }

        speak("Audio test", true)
    }

    fun isTestingArmed(): Boolean {
        val screen = Minecraft.getInstance().screen
        return screen is SelectDeviceScreen || screen is ConfigScreen
    }

    fun isSpeaking(): Boolean {
        return thread?.isSpeaking() ?: false
    }

    fun updateVoice(id: String? = ModState.config.voice.espeak.name) {
        val voice: String? = id ?: thread?.defaultVoice
        if (voice != null) {
            Espeak.setVoice(voice)
        } else {
            LOGGER.warn("Unable to get a new voice to update with")
        }
    }

    /** Gets the current voice (native call) */
    fun getVoice(): Espeak.Voice? {
        return Espeak.getVoice()
    }
}
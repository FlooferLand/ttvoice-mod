package com.flooferland.ttvoice.speech

import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.player.PlayerEntity
import org.jetbrains.annotations.CheckReturnValue

interface ISpeaker {
    // Types
    data class WorldContext(val player: PlayerEntity, val world: ClientWorld)
    sealed interface Status {
        class Success : Status
        data class Failure(val type: StatusType, val context: String? = null) : Status
    }
    enum class StatusType {
        VoiceMuted,
        VoiceDisconnected,
        NoOutputDevice,
        Internal
    }

    /** Converts text to audio and plays it */
    @CheckReturnValue
    public fun speak(text: String): Status

    /** Cuts the current audio */
    public fun shutUp()

    /** Check to see if the speaker is currently speaking */
    public fun isSpeaking(): Boolean

    /** Used for the audio output picker */
    public fun playTest() {
        speak("Audio test")
    }

    /** Called when the client loads into a world, or the settings screen */
    public fun load(context: WorldContext?) {}

    /** Called when unloading from a world */
    public fun unload() {}
}
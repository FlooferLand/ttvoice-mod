package com.flooferland.ttvoice.util

import com.flooferland.ttvoice.data.ModState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents

object SatisfyingNoises {
    fun playSuccess() {
        tryPlay(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1f)
    }
    fun playConfirm() {
        tryPlay(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 0.6f)
    }
    fun playDeny() {
        tryPlay(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.4f)
    }
    fun playClick(pitchMod: Float = 0.0f) {
        tryPlay(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1f, 1.0f + (pitchMod * 0.1f))
    }

    private fun tryPlay(event: SoundEvent, volume: Float=1f, pitch: Float=1f) {
        if (!ModState.config.ui.sounds) return
        val client = MinecraftClient.getInstance()
        client.soundManager.play(PositionedSoundInstance.master(event, pitch, volume * 0.4f))
    }
}
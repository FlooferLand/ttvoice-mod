package com.flooferland.ttvoice.util

import com.flooferland.ttvoice.data.ModState
import net.minecraft.client.*
import net.minecraft.client.resources.sounds.*
import net.minecraft.sounds.*

object SatisfyingNoises {
    fun playSuccess() {
        tryPlay(SoundEvents.ARROW_HIT_PLAYER, 1f)
    }
    fun playConfirm() {
        tryPlay(SoundEvents.ARROW_HIT_PLAYER, 0.6f)
    }
    fun playDeny() {
        tryPlay(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f)
    }
    fun playClick(pitchMod: Float = 0.0f) {
        tryPlay(SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 1.0f + (pitchMod * 0.1f))
    }

    private fun tryPlay(event: SoundEvent, volume: Float=1f, pitch: Float=1f) {
        if (!ModState.config.ui.sounds) return
        val client = Minecraft.getInstance()
        client.soundManager.play(SimpleSoundInstance.forUI(event, pitch, volume * 0.4f))
    }
}
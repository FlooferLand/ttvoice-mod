package com.flooferland.ttvoice.registry

import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.speech.SpeechUtil
import com.flooferland.ttvoice.util.SatisfyingNoises
import com.flooferland.ttvoice.util.Utils.noAudioMixerError
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.text.Text

object ModEvents {
    fun registerEvents() {
        ClientPlayConnectionEvents.JOIN.register() { handler, sender, client ->
            val player = client.player
            if (player == null) return@register

            SpeechUtil.load()
            if (ModState.config.audio.device == -1) {
                player.sendMessage(Text.of(noAudioMixerError()))
                SatisfyingNoises.playDeny()
            }
        }
        ClientPlayConnectionEvents.DISCONNECT.register() { handler, sender ->
            SpeechUtil.unload()
        }

        // Save config on mod exit
        ClientLifecycleEvents.CLIENT_STOPPING.register { client ->
            ModConfig.save()
        }
    }
}
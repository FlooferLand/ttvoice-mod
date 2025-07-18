package com.flooferland.ttvoice.registry

import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.util.Utils.noAudioMixerError
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.text.Text

object ModEvents {
    fun registerEvents() {
        ClientPlayConnectionEvents.JOIN.register() { handler, sender, client ->
            val player = client.player
            if (player == null) return@register

            if (ModState.config.audio.device == -1) {
                player.sendMessage(Text.of(noAudioMixerError()))
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register() { handler, sender ->

        }
    }
}
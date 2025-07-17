package com.flooferland.ttvoice

import de.maxhenkel.voicechat.api.VoicechatClientApi
import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.audiochannel.ClientAudioChannel
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent
import de.maxhenkel.voicechat.api.events.EventRegistration
import net.minecraft.client.MinecraftClient

class VcPlugin : VoicechatPlugin {
    override fun getPluginId(): String? {
        return TextToVoiceClient.MOD_ID
    }

    override fun registerEvents(registration: EventRegistration?) {
        if (registration == null) {
            TextToVoiceClient.LOGGER.error("Registration was null!")
            return;
        }
        registration.registerEvent(ClientVoicechatConnectionEvent::class.java, { packet ->
            api = packet.voicechat
            channel = api.createStaticAudioChannel(MinecraftClient.getInstance().player!!.uuid)
            //channel = api.createEntityAudioChannel(UUID(), api.fromEntity(MinecraftClient.getInstance().player))
        }, 10)
    }

    companion object {
        lateinit var api: VoicechatClientApi
        lateinit var channel: ClientAudioChannel
    }
}

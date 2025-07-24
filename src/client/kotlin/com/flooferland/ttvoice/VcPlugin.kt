package com.flooferland.ttvoice

import de.maxhenkel.voicechat.api.VoicechatClientApi
import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.audiochannel.ClientAudioChannel
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent
import de.maxhenkel.voicechat.api.events.EventRegistration
import net.minecraft.client.MinecraftClient

class VcPlugin : VoicechatPlugin {
    companion object {
        var api: VoicechatClientApi? = null
        var channel: ClientAudioChannel? = null

        val modName: String
            get() = "Simple Voice Chat"
        val muted: Boolean
            get() = api?.isMuted ?: false
        val connected: Boolean
            get() = api?.let { !it.isDisabled && !it.isDisconnected } ?: false
    }

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
            channel = api!!.createStaticAudioChannel(MinecraftClient.getInstance().player!!.uuid)
            //channel = api!!.createEntityAudioChannel(UUID(), api.fromEntity(MinecraftClient.getInstance().player))
        }, 10)
    }
}

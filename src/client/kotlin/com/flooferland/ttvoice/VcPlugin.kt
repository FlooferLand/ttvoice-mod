package com.flooferland.ttvoice

import com.flooferland.ttvoice.data.AudioScheduler
import com.flooferland.ttvoice.data.ModState
import de.maxhenkel.voicechat.api.VoicechatClientApi
import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.audiochannel.ClientAudioChannel
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent
import net.minecraft.client.Minecraft

public class VcPlugin : VoicechatPlugin {
    companion object {
        var api: VoicechatClientApi? = null
        var channel: ClientAudioChannel? = null
        val scheduler = AudioScheduler()

        val modName: String
            get() = "Simple Voice Chat"
        val muted: Boolean
            get() = api?.isMuted ?: false
        val connected: Boolean
            get() = api?.let { !it.isDisabled && !it.isDisconnected } ?: false

        fun sendFrame(frame: ShortArray) {
            scheduler.pushFrame(frame)
        }
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
            channel = api!!.createStaticAudioChannel(Minecraft.getInstance().player!!.uuid)
            //channel = api!!.createEntityAudioChannel(UUID.randomUUID(), api!!.fromEntity(Minecraft.getInstance().player))
        }, 10)

        // NOTE: SVC expects 48,000 hz audio at 16 bits mono. (consistent 960 frame size)
        //       Screw you SVC for being ridiculously undocumented
        registration.registerEvent(MergeClientSoundEvent::class.java, { packet ->
            val frame = scheduler.next() ?: return@registerEvent
            packet.mergeAudio(frame)
            if (ModState.config.general.hearSelf) {
                channel?.play(frame)
            }
        }, 10)
    }
}

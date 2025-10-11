package com.flooferland.ttvoice.figura

//? if has_figura {
import com.flooferland.ttvoice.TextToVoiceClient.Companion.LOGGER
import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.accessors.FiguraEventsAPIAccess
import com.mojang.datafixers.util.Pair
import org.figuramc.figura.FiguraMod
import org.figuramc.figura.avatar.AvatarManager
import org.figuramc.figura.entries.FiguraEvent
import org.figuramc.figura.entries.annotations.FiguraEventPlugin
import org.figuramc.figura.lua.LuaWhitelist
import org.figuramc.figura.lua.api.event.LuaEvent
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import kotlin.math.sqrt

@FiguraEventPlugin
class FiguraEventPlugin : FiguraEvent {
    override fun getID(): String = MOD_ID
    override fun getEvents(): Collection<Pair<String, LuaEvent>> =
        listOf(
            Pair(TTS_SPEAK_STRING, TTS_SPEAK),
            Pair(TTS_SPEAK_RAW_STRING, TTS_SPEAK_RAW)
        )

    companion object {
        const val TTS_SPEAK_STRING = "TTS_SPEAK"
        @LuaWhitelist var TTS_SPEAK = LuaEvent()

        const val TTS_SPEAK_RAW_STRING = "TTS_SPEAK_RAW"
        @LuaWhitelist var TTS_SPEAK_RAW = LuaEvent()

        const val ENVELOPE_BANDS = 20
        const val API_VERSION = 1  // NOTE: Bump this up any time there's a breaking change to the code below

        fun sendSpeakingEvent(frame: ShortArray = shortArrayOf()) {
            val avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID())
            if (avatar == null || avatar.luaRuntime == null) return

            val runtime = avatar.luaRuntime
            val events = (runtime.events as FiguraEventsAPIAccess)

            // Calculating RMS envelope
            val rmsEnvelopeResult = runCatching {
                val frameSize = frame.size
                val bandSize = frameSize / ENVELOPE_BANDS
                if (frameSize < ENVELOPE_BANDS) { return }

                FloatArray(ENVELOPE_BANDS) { band ->
                    val start = band * bandSize
                    val end = (start + bandSize).coerceAtMost(frameSize)

                    var sumSquared = 0f
                    for (i in 0 until end) {
                        frame[i].let { sumSquared += it * it }
                    }

                    // NOTE: If the audio bitrate is ever changed, Short.MAX_VALUE must be changed appropriately as well
                    val rawSample = sqrt(sumSquared / (end - start))
                    (rawSample / Short.MAX_VALUE.toFloat()).coerceIn(0f..1f)
                }
            }
            val rmsEnvelope = rmsEnvelopeResult.getOrNull() ?: FloatArray(ENVELOPE_BANDS)
            rmsEnvelopeResult.onFailure { err ->
                LOGGER.error("Error occurred while calculating RMS envelope: $err")
            }

            // TODO: Add pitch estimation

            // Building a Lua table
            val rmsEnvelopeTable = LuaTable.listOf(rmsEnvelope.map { LuaValue.valueOf(it.toDouble()) }.toTypedArray())
            val rawTable = LuaTable.listOf(frame.map { LuaValue.valueOf(it.toDouble()) }.toTypedArray())

            // Sending the events
            avatar.run(events.speakEvent, avatar.tick, rmsEnvelopeTable)
            avatar.run(events.speakRawEvent, avatar.tick, rawTable)
        }
    }
}
//?} else {
/*class FiguraEventPlugin {
    companion object {
        fun sendSpeakingEvent(frame: ShortArray = shortArrayOf()) {

        }
    }
}
*///?}

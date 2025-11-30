package com.flooferland.ttvoice.util

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import kotlin.math.roundToInt

object Extensions {
    /** Changes the sample rate of an audio buffer */
    public fun ShortArray.resampleRate(input: Int, output: Int): ShortArray {
        val ratio = output.toDouble() / input
        val outSize = (size * ratio).roundToInt()
        return ShortArray(outSize) { v ->
            val srcPos = v / ratio
            val i0 = srcPos.toInt().coerceIn(indices)
            val i1 = (i0 + 1).coerceAtMost(lastIndex)
            Utils.lerp(this[i0], this[i1], srcPos - i0)
        }
    }

    public fun MutableComponent.compatHoverTooltip(tooltip: Component): MutableComponent = this
        //? if <1.21.7 {
        .withStyle { s -> s.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)) }
        //?} else {
        /*.withStyle { s -> s.withHoverEvent(HoverEvent.ShowText(tooltip)) }
        *///?}
}
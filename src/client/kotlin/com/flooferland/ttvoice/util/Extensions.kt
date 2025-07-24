package com.flooferland.ttvoice.util

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
}
@file:Suppress("unused")

package com.flooferland.ttvoice.util

import kotlin.math.roundToInt

object Utils {
    fun noAudioMixerError(): String {
        return "Please select an audio mixer using /ttvoice mixer set"
    }

    fun lerp(a: Double, b: Double, t: Double): Double =
        a * (1 - t) + b * t
    fun lerp(a: Short, b: Short, t: Double): Short =
        (a + (b - a) * t).roundToInt().toShort()
}
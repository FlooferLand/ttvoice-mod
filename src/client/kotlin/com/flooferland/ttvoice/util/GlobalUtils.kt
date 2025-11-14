package com.flooferland.ttvoice.util

import com.flooferland.ttvoice.TextToVoiceClient
import net.minecraft.resources.*

/** Creates a [ResourceLocation] using the [com.flooferland.ttvoice.TextToVoiceClient.MOD_ID] namespace */
fun rl(path: String) = rlCustom(TextToVoiceClient.MOD_ID, path)

/** Creates a [ResourceLocation] using the vanilla Minecraft namespace */
fun rlVanilla(path: String) = rlCustom(ResourceLocation.DEFAULT_NAMESPACE, path)

/** Creates a [ResourceLocation] using a custom namespace */
fun rlCustom(namespace: String, path: String) = ResourceLocation.tryBuild(namespace, path)!!

fun <E> MutableList<E>.copy(): MutableList<E> {
    return ArrayList(this)
}

fun lerp(a: Double, b: Double, t: Double): Double {
    return a * (1.0 - t) + b * t
}
fun lerp(a: Float, b: Float, t: Float): Float {
    return a * (1.0f - t) + b * t
}

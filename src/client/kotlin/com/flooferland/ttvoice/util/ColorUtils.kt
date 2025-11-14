package com.flooferland.ttvoice.util

object ColorUtils {
    fun getColor(r: Int, g: Int, b: Int, a: Int = 255): Int {
        return getColorArgb(a, r, g, b)
    }
    fun getColorArgb(a: Int, r: Int, g: Int, b: Int): Int {
        //? if >1.21.1 {
        /*return a shl 24 or (r shl 16) or (g shl 8) or b
        *///?} else {
        return net.minecraft.util.FastColor.ARGB32.color(a, r, g, b)
        //?}
    }
}
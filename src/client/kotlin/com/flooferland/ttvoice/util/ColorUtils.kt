package com.flooferland.ttvoice.util

import net.minecraft.util.math.ColorHelper

object ColorUtils {
    fun getColor(r: Int, g: Int, b: Int, a: Int = 255): Int {
        return getColorArgb(a, r, g, b)
    }
    fun getColorArgb(a: Int, r: Int, g: Int, b: Int): Int {
        //? if >1.21.1 {
        /*return ColorHelper.getArgb(a, r, g, b)
        *///?} else {
        return ColorHelper.Argb.getArgb(a, r, g, b)
        //?}
    }
}
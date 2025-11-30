package com.flooferland.ttvoice.loader

import java.nio.file.Path

interface ILoaderUtils {
    fun getDataDir(): Path
    fun isFiguraInstalled(): Boolean
}
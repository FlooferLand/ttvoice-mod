//? if fabric {
package com.flooferland.ttvoice.loader.fabric

import com.flooferland.ttvoice.TextToVoiceClient.MOD_ID
import com.flooferland.ttvoice.loader.ILoaderUtils
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader

object LoaderUtils : ILoaderUtils {
    override fun getDataDir(): Path = FabricLoader.getInstance().gameDir.resolve(MOD_ID)
    override fun isFiguraInstalled() = FabricLoader.getInstance().isModLoaded("figura")
}
//?}
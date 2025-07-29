package com.flooferland.ttvoice.mixin

import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

class MixinPlugin : IMixinConfigPlugin {
    override fun onLoad(p0: String?) = Unit
    override fun getRefMapperConfig(): String? = null
    override fun getMixins(): List<String?>? = null

    override fun shouldApplyMixin(targetClassName: String?, mixinName: String?): Boolean {
        if (targetClassName == null || mixinName == null) return false
        val shortName = mixinName.replace("com.flooferland.ttvoice.", "")

        // Figura conditional compilation
        if (shortName.startsWith("Figura") && shortName.endsWith("Mixin")) {
            return FabricLoader.getInstance().isModLoaded("figura")
        }
        return true
    }

    override fun acceptTargets(p0: Set<String?>?, p1: Set<String?>?) {}
    override fun preApply(p0: String?, p1: ClassNode?, p2: String?, p3: IMixinInfo?) {}
    override fun postApply(p0: String?, p1: ClassNode?, p2: String?, p3: IMixinInfo?) {}
}

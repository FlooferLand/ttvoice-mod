package com.flooferland.ttvoice.resource

import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.data.ModData
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import kotlin.jvm.optionals.getOrNull

object PythonResourceReloader : SimpleSynchronousResourceReloadListener, IdentifiableResourceReloadListener {
    override fun getFabricId(): Identifier? {
        return Identifier.of(MOD_ID, "PythonResourceReloader")
    }

    override fun reload(manager: ResourceManager?) {
        manager ?: return
        val file = manager.getResource(Identifier.of(MOD_ID, "tts.py"))
        val data = file.getOrNull()
        if (data == null) return
        ModData.pythonScript = data.inputStream.readBytes().toString(Charsets.UTF_8);
    }
}
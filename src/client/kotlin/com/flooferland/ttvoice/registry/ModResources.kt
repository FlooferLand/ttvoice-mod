package com.flooferland.ttvoice.registry

import com.flooferland.ttvoice.resource.PythonResourceReloader
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resource.ResourceType

object ModResources {
    fun registerReloaders() {
        val clientResources = ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
        clientResources.registerReloadListener(PythonResourceReloader)
    }
}
//? if fabric {
package com.flooferland.ttvoice.loader.fabric

import com.flooferland.ttvoice.TextToVoiceClient
import net.fabricmc.api.ClientModInitializer

class FabricClientEntrypoint : ClientModInitializer {
    override fun onInitializeClient() {
        TextToVoiceClient.onInitializeClient()
    }
}
//?}

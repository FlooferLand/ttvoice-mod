package com.flooferland.ttvoice.screen.widgets

import com.flooferland.ttvoice.VcPlugin
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.registry.ModCommands
import com.flooferland.ttvoice.screen.SelectDeviceScreen
import com.flooferland.ttvoice.screen.SpeechScreen
import com.flooferland.ttvoice.util.SatisfyingNoises
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.*
import net.minecraft.util.Formatting
import net.minecraft.util.math.MathHelper
import javax.sound.sampled.AudioSystem

class SpeechInfoLabelWidget(val screen: SpeechScreen, val textRenderer: TextRenderer) : PressableWidget(0, 0, 300, 15, Text.of("")) {
    override fun onPress() {
        val error = screen.error
        if (error != null) {
            SatisfyingNoises.playDeny()
            return
        }

        val player = MinecraftClient.getInstance().player
        if (player == null) return

        if (ModState.config.general.routeThroughDevice) {
            val devices = AudioSystem.getMixerInfo()
            if (ModState.config.audio.device < devices.size) {
                MinecraftClient.getInstance().setScreen(SelectDeviceScreen(screen))
            }
        }
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
    }

    override fun renderButton(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        context!!.drawTextWithShadow(this.textRenderer, message, this.getX(), this.getY(), 16777215 or (MathHelper.ceil(this.alpha * 255.0f) shl 24))
    }

    fun update() {
        val devices = AudioSystem.getMixerInfo()
        var text: MutableText = Text.empty()

        if (ModState.config.general.routeThroughDevice) {
            text.append(Text.literal("Device: ${devices.getOrNull(ModState.config.audio.device)}"))
            tooltip = Tooltip.of(Text.of("Click to change the device"))
        }
        if (ModState.config.general.routeThroughVoiceChat) {
            val connected = (if (VcPlugin.connected) "connected" else "disconnected")
            if (!text.string.isEmpty()) text.append("\n")
            text.append(Text.translatable("status.ttvoice.vc.$connected", VcPlugin.modName))
        }

        // Overrides
        if (screen.error?.message != null) {
            text = Text.literal(screen.error!!.message!!)
                .setStyle(Style.EMPTY.withFormatting(Formatting.DARK_RED))
            tooltip = Tooltip.of(Text.of("Error"))
        }

        message = text
    }

    fun setBenchmarkResult(startMillis: Long, endMillis: Long) {
        val string = "Seconds passed: ${(endMillis - startMillis) / 1000f}"
        message = Text.literal(string)
        println(string)
    }
}
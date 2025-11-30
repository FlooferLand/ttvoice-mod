package com.flooferland.ttvoice.screen.widgets

import com.flooferland.ttvoice.VcPlugin
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.screen.SelectDeviceScreen
import com.flooferland.ttvoice.screen.SpeechScreen
import com.flooferland.ttvoice.util.SatisfyingNoises
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.Mth
import javax.sound.sampled.AudioSystem

class SpeechInfoLabelWidget(val screen: SpeechScreen, val font: Font) : AbstractButton(0, 0, 300, 15, Component.literal("")) {
    fun onPressed() {
        val error = screen.error
        if (error != null) {
            SatisfyingNoises.playDeny()
            return
        }

        if (ModState.config.general.routeThroughDevice) {
            val devices = AudioSystem.getMixerInfo()
            if (ModState.config.audio.device < devices.size) {
                Minecraft.getInstance().setScreen(SelectDeviceScreen(screen))
            }
        }
    }

    fun render(context: GuiGraphics?) {
        context!!.drawString(this.font, message, this.getX(), this.getY(), 16777215 or (Mth.ceil(this.alpha * 255.0f) shl 24))
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}

    //? if >1.21.7 {
    /*override fun onPress(input: net.minecraft.client.input.InputWithModifiers) = onPressed()
    *///?} else {
    override fun onPress() = onPressed()
    //?}

    override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) = render(context)

    fun update() {
        val devices = AudioSystem.getMixerInfo()
        var text = Component.empty()

        if (ModState.config.general.routeThroughDevice) {
            text.append(Component.literal("Device: ${devices.getOrNull(ModState.config.audio.device)}"))
            setTooltip(Tooltip.create(Component.literal("Click to change the device")))
        }
        if (ModState.config.general.routeThroughVoiceChat) {
            val connected = (if (VcPlugin.connected) "connected" else "disconnected")
            if (!text.string.isEmpty()) text.append("\n")
            text.append(Component.translatable("status.ttvoice.vc.$connected", VcPlugin.modName))
        }

        // Overrides
        if (screen.error?.message != null) {
            text = Component.literal(screen.error!!.message!!)
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.DARK_RED))
            setTooltip(Tooltip.create(Component.literal("Error")))
        }

        message = text
    }

    fun setBenchmarkResult(startMillis: Long, endMillis: Long) {
        val string = "Seconds passed: ${(endMillis - startMillis) / 1000f}"
        message = Component.literal(string)
        println(string)
    }
}
package com.flooferland.ttvoice.mixin;

import com.flooferland.ttvoice.VcPlugin;
import com.flooferland.ttvoice.data.ModState;
import com.flooferland.ttvoice.speech.SpeechUtil;
import de.maxhenkel.voicechat.voice.client.RenderEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderEvents.class)
public class SVCSpeakingVisualMixin {
	@Inject(
		method = "onRenderHUD",
		at = @At(value = "HEAD")
	)
	public void ttvoice$forceMicIcon(GuiGraphics guiGraphics, float tickDelta, CallbackInfo ci) {
		var api = VcPlugin.Companion.getApi();
		if (api == null) return;
		boolean hideIcons = api.getClientConfig().getBoolean("hideIcons", false);
		boolean canSpeak = VcPlugin.Companion.getConnected() && !VcPlugin.Companion.getMuted() && ModState.config.getGeneral().getRouteThroughVoiceChat();

		var accessor = ((RenderEventsAccessor) (Object) this);
		if (accessor.callShouldShowIcons() && !hideIcons && canSpeak) {
			if (SpeechUtil.INSTANCE.isSpeaking()) {
				accessor.callRenderIcon(guiGraphics, RenderEventsAccessor.getSpeakerIcon());
			}
		}
	}
}

@Mixin(RenderEvents.class)
interface RenderEventsAccessor {
	@Invoker(value = "shouldShowIcons", remap = false)
	boolean callShouldShowIcons();

	@Invoker("renderIcon")
	void callRenderIcon(GuiGraphics guiGraphics, ResourceLocation texture);

	@Accessor("MICROPHONE_ICON")
	static ResourceLocation getSpeakerIcon() { return null; }
}

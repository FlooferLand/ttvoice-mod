package com.flooferland.ttvoice.mixin;

import com.flooferland.ttvoice.VcPlugin;
import com.flooferland.ttvoice.speech.SpeechUtil;
import de.maxhenkel.voicechat.voice.client.RenderEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
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
	public void ttvoice$forceMicIcon(DrawContext guiGraphics, float tickDelta, CallbackInfo ci) {
		var api = VcPlugin.Companion.getApi();
		if (api == null) return;
		boolean hideIcons = api.getClientConfig().getBoolean("hideIcons", false);

		var accessor = ((RenderEventsAccessor) (Object) this);
		System.out.println(SpeechUtil.INSTANCE.isSpeaking());
		if (SpeechUtil.INSTANCE.isSpeaking()) {
			accessor.callRenderIcon(guiGraphics, RenderEventsAccessor.getSpeakerIcon());
		}
		if (accessor.callShouldShowIcons() && hideIcons != true && VcPlugin.Companion.getConnected() && !VcPlugin.Companion.getMuted()) {

		}
	}
}

@Mixin(RenderEvents.class)
interface RenderEventsAccessor {
	@Invoker(value = "shouldShowIcons", remap = false)
	boolean callShouldShowIcons();

	@Invoker("renderIcon")
	void callRenderIcon(DrawContext guiGraphics, Identifier texture);

	@Accessor("MICROPHONE_ICON")
	static Identifier getSpeakerIcon() { return null; }
}

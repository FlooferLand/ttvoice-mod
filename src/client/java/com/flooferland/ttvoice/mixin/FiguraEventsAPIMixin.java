package com.flooferland.ttvoice.mixin;

import com.flooferland.ttvoice.accessors.FiguraEventsAPIAccess;
import com.flooferland.ttvoice.figura.FiguraEventPlugin;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.event.EventsAPI;
import org.figuramc.figura.lua.api.event.LuaEvent;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = EventsAPI.class, remap = false)
@Implements(@Interface(iface = FiguraEventsAPIAccess.class, prefix = "ttvoice$"))
public class FiguraEventsAPIMixin {
	@Shadow @Final private Map<String, LuaEvent> events;

	@Unique @LuaWhitelist @LuaFieldDoc("events.tts_speak")
	public LuaEvent TTS_SPEAK = new LuaEvent();

	@Unique @LuaWhitelist @LuaFieldDoc("events.tts_speak_raw")
	public LuaEvent TTS_SPEAK_RAW = new LuaEvent();

	@Inject(method = "<init>", at = @At("RETURN"))
	void a(CallbackInfo ci) {
		events.put(FiguraEventPlugin.TTS_SPEAK_STRING, TTS_SPEAK);
		events.put(FiguraEventPlugin.TTS_SPEAK_RAW_STRING, TTS_SPEAK_RAW);
	}

	public LuaEvent ttvoice$getSpeakEvent() {
		return TTS_SPEAK;
	}
	public LuaEvent ttvoice$getSpeakRawEvent() {
		return TTS_SPEAK_RAW;
	}
}

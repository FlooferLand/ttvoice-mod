package com.flooferland.ttvoice.mixin;

import com.flooferland.ttvoice.figura.TextToVoiceAPI;
import org.figuramc.figura.lua.FiguraAPIManager;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Mixin(value = FiguraAPIManager.class, remap = false)
public class FiguraAPIManagerMixin {
	@Shadow @Final
	public static Set<Class<?>> WHITELISTED_CLASSES;

	@Shadow @Final
	public static Map<String, Function<FiguraLuaRuntime, Object>> API_GETTERS;

	static {
		WHITELISTED_CLASSES.add(TextToVoiceAPI.class);
		API_GETTERS.put("ttvoice", TextToVoiceAPI::new);
	}
}
package com.flooferland.ttvoice.mixin;

import com.flooferland.ttvoice.figura.TextToVoiceAPI;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.spongepowered.asm.mixin.Mixin;
import org.figuramc.figura.lua.docs.FiguraGlobalsDocs;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = FiguraGlobalsDocs.class, remap = false)
public class FiguraGlobalDocsMixin {
	@Unique	@LuaFieldDoc("globals.ttvoice")
	public TextToVoiceAPI ttvoice;
}

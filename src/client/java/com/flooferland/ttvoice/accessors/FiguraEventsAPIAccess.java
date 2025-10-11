package com.flooferland.ttvoice.accessors;

//? if has_figura {
import org.figuramc.figura.lua.api.event.LuaEvent;

public interface FiguraEventsAPIAccess {
	public LuaEvent getSpeakEvent();
	public LuaEvent getSpeakRawEvent();
}
//?}

package com.flooferland.ttvoice.figura

//? if has_figura {
import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import org.figuramc.figura.avatar.Avatar
import org.figuramc.figura.entries.FiguraAPI
import org.figuramc.figura.entries.annotations.FiguraAPIPlugin
import org.figuramc.figura.lua.FiguraLuaRuntime
import org.figuramc.figura.lua.LuaWhitelist
import org.figuramc.figura.lua.docs.LuaFieldDoc
import org.figuramc.figura.lua.docs.LuaMethodDoc
import org.figuramc.figura.lua.docs.LuaTypeDoc

@FiguraAPIPlugin
@LuaWhitelist
@LuaTypeDoc(name = "TextToVoiceAPI", value = "ttvoice")
class TextToVoiceAPI : FiguraAPI {
    override fun getName(): String = MOD_ID

    val runtime: FiguraLuaRuntime?
    val owner: Avatar?

    constructor() {
        this.runtime = null
        this.owner = null
    }

    constructor(runtime: FiguraLuaRuntime?) {
        this.runtime = runtime
        this.owner = runtime?.owner
    }

    override fun build(avatar: Avatar): FiguraAPI {
        return TextToVoiceAPI(avatar.luaRuntime)
    }
    override fun getWhitelistedClasses(): Collection<Class<*>> {
        return listOf(TextToVoiceAPI::class.java)
    }
    override fun getDocsClasses(): Collection<Class<*>> {
        return listOf()
    }

    @LuaWhitelist
    @LuaMethodDoc("ttvoice.getApiVersion")
    fun getApiVersion(): Int = FiguraEventPlugin.API_VERSION
}
//?}

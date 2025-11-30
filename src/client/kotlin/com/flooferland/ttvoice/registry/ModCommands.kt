package com.flooferland.ttvoice.registry

import com.flooferland.ttvoice.TextToVoiceClient.MOD_ID
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.speech.SpeechUtil
import com.flooferland.ttvoice.util.SatisfyingNoises
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.*
import net.minecraft.commands.*
import net.minecraft.network.chat.*
import javax.sound.sampled.AudioSystem

@Suppress("unused")
object ModCommands {
    enum class Commands(path: String) {
        Speak("speak"),
        StopSpeaking("stopSpeaking"),
        VoiceSet("voice set"),
        VoiceSetExact("voice setExact"),
        MixerSet("mixer set"),
        MixerSetExact("mixer setExact");

        val command: String
        val subcommand: String?
        init {
            val path = path.split(" ");
            command = path[0]
            subcommand = path.getOrNull(1)
        }

        fun withParams(vararg params: Any): String {
            val command = getFullPath()
            if (params.isEmpty())
                return command
            return "$command ${params.joinToString(" ")}"
        }
        fun getFullPath(): String {
            return "/$MOD_ID $command" + (if (subcommand != null) " $subcommand" else "")
        }
    }

    fun speakCommand(context: CommandContext<FabricClientCommandSource>): Int {
        val text = getString(context, "text")
        SpeechUtil.speak(text)

        // TODO: Add playing the audio for the client via Minecraft's audio API
        // https://github.com/walksanatora/aeiou-mc/blob/1.20/src/client/java/net/walksanator/aeiou/AeiouModClient.java#L41
        return 1
    }
    fun stopSpeaking(context: CommandContext<FabricClientCommandSource>): Int {
        SpeechUtil.shutUp()
        return 1
    }

    fun setVoice(context: CommandContext<FabricClientCommandSource>): Int {
        val voice = getString(context, "voice")
        // TODO
        return 1
    }

    fun setMixer(context: CommandContext<FabricClientCommandSource>): Int {
        val mixer = getInteger(context, "mixer")
        val mixerInfo = AudioSystem.getMixerInfo()
        if (mixer < 0 || mixer > mixerInfo.size - 1) {
            context.source.sendError(Component.literal("Selected device doesn't exist"))
            return 0
        }
        SatisfyingNoises.playSuccess()
        ModState.config.audio.device = mixer
        context.source.sendFeedback(
            Component.literal("\nAudio mixer was successfully set to ")
                //? if <1.21 {
                .append(
                    Component.literal(mixerInfo[mixer].name)
                        .setStyle(Style.EMPTY.withBold(true)
                        .withHoverEvent(HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal("Device ID $mixer"))))
                )
                //?}
        )
        return 1
    }

    fun setMixerFromList(context: CommandContext<FabricClientCommandSource>): Int {
        val mixerText = Component.empty()
        val mixerInfo = AudioSystem.getMixerInfo()
        for ((i, mixer) in mixerInfo.withIndex()) {
            val isVoicemeeterOutMixer = mixer.description.lowercase().contains("port mixer")
            mixerText.append(Component.literal("- "));
            mixerText.append(
                Component.literal(mixer.name)
                    .setStyle(
                        Style.EMPTY
                            .withBold(isVoicemeeterOutMixer)
                            //? if <1.21 {
                            .withColor(if (isVoicemeeterOutMixer) ChatFormatting.GREEN else ChatFormatting.WHITE)
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("${mixer.description} (Device ID $i)")))
                            .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, Commands.MixerSetExact.withParams(i)))
                            //?}
                    )
            )
            mixerText.append(Component.literal("\n"))
        }
        context.source.sendFeedback(mixerText)
        return 1
    }

    fun setVoiceFromList(context: CommandContext<FabricClientCommandSource>): Int {
        val voiceText = Component.empty()
        for ((i, voice) in SpeechUtil.getVoices().withIndex()) {
            voiceText.append(Component.literal("- "));
            voiceText.append(
                Component.literal(voice.name)
                    .setStyle(
                        Style.EMPTY
                            //? if <1.21 {
                            .withColor(ChatFormatting.WHITE)
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("${voice.name} (id=${voice.identifier})")))
                            .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, Commands.VoiceSetExact.withParams(i)))
                            //?}
                    )
            )
            voiceText.append(Component.literal("\n"))
        }
        context.source.sendFeedback(voiceText)
        return 1
    }

    fun registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register() { dispatcher, registryAccess ->
            val speakCommandLiteral = ClientCommandManager.literal(Commands.Speak.command)
                .executes { context ->
                    context.source.sendError(Component.literal("Provide the text to speak with!"));
                    return@executes 0
                }
                .then(
                    argument("text", greedyString())
                        .executes(ModCommands::speakCommand)
                )
            val stopSpeakingCommandLiteral = ClientCommandManager.literal(Commands.StopSpeaking.command)
                .executes(ModCommands::stopSpeaking)

            val ttvoice = dispatcher.register(
                ClientCommandManager.literal(MOD_ID)
                    .then(speakCommandLiteral)
                    .then(stopSpeakingCommandLiteral)
                    .then(
                        ClientCommandManager.literal(Commands.MixerSet.command)
                            .then(
                                ClientCommandManager.literal(Commands.MixerSetExact.subcommand).then(
                                    argument("mixer", integer())
                                        .executes(ModCommands::setMixer)
                                        .suggests({ context, builder ->
                                            val mixers = AudioSystem.getMixerInfo().mapIndexed { i, mixer -> "($i) ${mixer.name}" }
                                            SharedSuggestionProvider.suggest(mixers, builder)
                                        })
                                )
                            )
                            .then(
                                ClientCommandManager.literal(Commands.MixerSet.subcommand)
                                    .executes(ModCommands::setMixerFromList)
                            )
                    )
                    .then(
                        ClientCommandManager.literal(Commands.VoiceSet.command)
                            .then(
                                ClientCommandManager.literal(Commands.VoiceSetExact.subcommand).then(
                                    argument("voice", integer())
                                        .executes(ModCommands::setVoice)
                                        .suggests({ context, builder ->
                                            val mixers = SpeechUtil.getVoices().mapIndexed { i, voice -> "($i) ${voice.name}" }
                                            SharedSuggestionProvider.suggest(mixers, builder)
                                        })
                                )
                            )
                            .then(
                                ClientCommandManager.literal(Commands.VoiceSet.subcommand)
                                    .executes(ModCommands::setVoiceFromList)
                            )
                    )
            )

            // TODO: Add aliases
            // dispatcher.register(literal("s").redirect(speak.redirect));
            dispatcher.register(speakCommandLiteral)
            dispatcher.register(stopSpeakingCommandLiteral)
        }
    }
}
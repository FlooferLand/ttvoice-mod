package com.flooferland.ttvoice.registry

import com.flooferland.ttvoice.speech.SpeechUtil
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.util.SatisfyingNoises
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.string
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandSource
import net.minecraft.sound.SoundEvents
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.math.ColorHelper
import javax.sound.sampled.AudioSystem

object ModCommands {
    fun speakCommand(context: CommandContext<FabricClientCommandSource>): Int {
        val text = getString(context, "text")
        SpeechUtil.speak(text)

        // TODO: Add playing the audio for the client via Minecraft's audio API
        // https://github.com/walksanatora/aeiou-mc/blob/1.20/src/client/java/net/walksanator/aeiou/AeiouModClient.java#L41
        return 1
    }
    fun stopSpeaking(context: CommandContext<FabricClientCommandSource>): Int {
        SpeechUtil.stopSpeaking()
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
            context.source.sendError(Text.of("Selected device doesn't exist"))
            return 0
        }
        SatisfyingNoises.playSuccess()
        ModState.config.audio.device = mixer
        context.source.sendFeedback(
            Text.literal("\nAudio mixer was successfully set to ")
                .append(
                    Text.literal(mixerInfo[mixer].name)
                        .setStyle(Style.EMPTY.withBold(true)
                        .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Device ID $mixer"))))
                )
        )
        return 1
    }

    fun setMixerFromList(context: CommandContext<FabricClientCommandSource>): Int {
        val mixerText = Text.empty()
        val mixerInfo = AudioSystem.getMixerInfo()
        for ((i, mixer) in mixerInfo.withIndex()) {
            val isVoicemeeterOutMixer = mixer.description.lowercase().contains("port mixer")
            mixerText.append(Text.of("- "));
            mixerText.append(
                Text.literal(mixer.name)
                    .setStyle(
                        Style.EMPTY
                            .withBold(isVoicemeeterOutMixer)
                            .withColor(if (isVoicemeeterOutMixer) ColorHelper.Argb.getArgb(255, 150, 255, 150) else Colors.WHITE)
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("${mixer.description} (Device ID $i)")))
                            .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ttvoice mixer setExact $i"))
                    )
            )
            mixerText.append(Text.of("\n"))
        }
        context.source.sendFeedback(mixerText)
        return 1
    }

    fun registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register() { dispatcher, registryAccess ->
            val speakCommandLiteral = ClientCommandManager.literal("speak")
                .executes { context ->
                    context.source.sendError(Text.of("Provide the text to speak with!"));
                    return@executes 0
                }
                .then(
                    argument("text", string())
                        .executes(ModCommands::speakCommand)
                )
            val stopSpeakingCommandLiteral = ClientCommandManager.literal("stopSpeaking")
                .executes(ModCommands::stopSpeaking)

            val ttvoice = dispatcher.register(
                ClientCommandManager.literal("ttvoice")
                    .then(speakCommandLiteral)
                    .then(stopSpeakingCommandLiteral)
                    .then(
                        ClientCommandManager.literal("voice")
                            .then(
                                ClientCommandManager.literal("set").then(
                                    argument("voice", string())
                                        .executes(ModCommands::setVoice)
                                        /*.suggests({ context, builder ->
                                            CommandSource.suggestMatching(mary?.availableVoices, builder)
                                        })*/
                                )
                            )
                    )
                    .then(
                        ClientCommandManager.literal("mixer")
                            .then(
                                ClientCommandManager.literal("setExact").then(
                                    argument("mixer", integer())
                                        .executes(ModCommands::setMixer)
                                        .suggests({ context, builder ->
                                            val mixers = AudioSystem.getMixerInfo().map { mixer -> mixer.name }
                                            CommandSource.suggestMatching(mixers, builder)
                                        })
                                )
                            )
                            .then(
                                ClientCommandManager.literal("set")
                                    .executes(ModCommands::setMixerFromList)
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
package com.flooferland.ttvoice

import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.flooferland.ttvoice.data.ModConfig
import com.flooferland.ttvoice.util.ModState
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.Optional
import javax.sound.sampled.AudioSystem
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@Environment(EnvType.CLIENT)
class ModMenuEntry : ModMenuApi {
    val manuallyDisplayed = arrayListOf<String>()
    val categoryMap = hashMapOf<String, ConfigCategory>()

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent: Screen? ->
            val builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.${MOD_ID}.config"))
                .setSavingRunnable({
                    // TODO: Manually save the config here, and add manual config loading
                })
            val entryBuilder = builder.entryBuilder()

            // region | General stuff
            val generalCategory = builder.getOrCreateCategory(Text.of("General"))
                .setCategoryBackground(Identifier.of("minecraft", "block/stone"))
            categoryMap.put(ModConfig::general.name, generalCategory)
            // endregion

            // region | Audio category
            val audioCategory = builder.getOrCreateCategory(Text.of("Audio"))
                .setCategoryBackground(Identifier.of("minecraft", "note_block"))
            categoryMap.put(ModConfig::audio.name, audioCategory)
            run {
                // Devices
                val mixerInfo = AudioSystem.getMixerInfo()
                val mixerNames = mixerInfo.map { v -> v.name }
                val devices = entryBuilder
                    .startDropdownMenu(
                        Text.translatable("config.${MOD_ID}.field.audio.devices"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(
                            ModState.config.audio.device,
                            { s -> mixerInfo.indexOfFirst { v -> (v.name == s) } },
                            { i -> Text.of(mixerNames[i as Int]) }),
                        DropdownBoxEntry.DefaultSelectionCellCreator()
                    )
                    .setSelections(mixerNames.indices)
                    .setErrorSupplier { p ->
                        if (p < 0 || p >= mixerInfo.size)
                            return@setErrorSupplier Optional.of(Text.of("Out of range"))
                        else
                            return@setErrorSupplier Optional.empty()
                    }
                    .build()
                audioCategory.addEntry(devices)
                addManuallyDisplayed(ModConfig.AudioConfig::device)
            }
            // endregion

            // Auto-GUI
            for (categoryProp in ModConfig::class.memberProperties) {
                val categoryName = categoryProp.name
                val categoryValue = categoryProp.get(ModState.config) ?: continue
                val category = categoryMap.getOrElse(categoryName) {
                    TextToVoiceClient.LOGGER.error("Category mapping for $categoryName not found in config")
                    continue
                }

                for (field in categoryValue::class.memberProperties) {
                    val fieldName = field.name
                    val fieldValue = (field as KProperty1<Any, *>).get(categoryValue) ?: continue

                    if (isManuallyDisplayed<Any, Any>(field)) {
                        continue
                    }

                    val type = field.returnType;
                    when (type.classifier) {
                        Boolean::class -> {
                            val thing = entryBuilder
                                .startBooleanToggle(Text.translatable("config.${MOD_ID}.field.${categoryName}.${fieldName}"), fieldValue as Boolean)
                                .build()
                            category.addEntry(thing)
                        }
                        String::class -> {
                            val thing = entryBuilder
                                .startStrField(Text.translatable("config.${MOD_ID}.field.${categoryName}.${fieldName}"), fieldValue as String)
                                .build()
                            category.addEntry(thing)
                        }
                        else -> {
                            println("Unknown type in config ${categoryName}.${fieldName} (${type.classifier})")
                        }
                    }
                }
            }

            builder.build()
        }
    }

    private inline fun<reified K, V> addManuallyDisplayed(property: KProperty1<K, V>) {
        manuallyDisplayed.add("${K::class.qualifiedName}.${property.name}")
    }
    private inline fun<reified K, V> isManuallyDisplayed(property: KProperty1<Any, *>): Boolean {
        return manuallyDisplayed.contains("${K::class.qualifiedName}.${property.name}")
    }
}

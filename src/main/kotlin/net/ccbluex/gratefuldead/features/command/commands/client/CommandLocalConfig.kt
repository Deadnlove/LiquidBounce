/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.gratefuldead.features.command.commands.client

import com.mojang.blaze3d.Blaze3D
import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.async
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.gratefuldead.api.core.ioScope
import net.ccbluex.gratefuldead.config.ConfigSystem
import net.ccbluex.gratefuldead.config.OptionalInclusion
import net.ccbluex.gratefuldead.config.autoconfig.AutoConfig
import net.ccbluex.gratefuldead.config.autoconfig.AutoConfig.serializeAutoConfig
import net.ccbluex.gratefuldead.config.autoconfig.AutoConfigMetadata
import net.ccbluex.gratefuldead.config.autoconfig.IncludeConfiguration
import net.ccbluex.gratefuldead.config.gson.publicGson
import net.ccbluex.gratefuldead.config.gson.util.int
import net.ccbluex.gratefuldead.config.gson.util.parseTree
import net.ccbluex.gratefuldead.features.command.CommandException
import net.ccbluex.gratefuldead.features.command.CommandManager
import net.ccbluex.gratefuldead.features.command.CommandRegistrar
import net.ccbluex.gratefuldead.features.command.arguments.BooleanArgumentType
import net.ccbluex.gratefuldead.features.command.arguments.ClientStringArgumentType
import net.ccbluex.gratefuldead.features.command.arguments.MultiSelectArgumentType
import net.ccbluex.gratefuldead.features.command.arguments.MultiTaggedArgumentType
import net.ccbluex.gratefuldead.features.command.brigadier.ClientCommandSource
import net.ccbluex.gratefuldead.features.command.brigadier.CmdI18n
import net.ccbluex.gratefuldead.features.command.brigadier.get
import net.ccbluex.gratefuldead.features.command.brigadier.register
import net.ccbluex.gratefuldead.features.command.brigadier.suggestions
import net.ccbluex.gratefuldead.features.command.preset.pagedList
import net.ccbluex.gratefuldead.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleManager
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.clickablePath
import net.ccbluex.gratefuldead.utils.client.highlight
import net.ccbluex.gratefuldead.utils.client.logger
import net.ccbluex.gratefuldead.utils.client.markAsError
import net.ccbluex.gratefuldead.utils.client.onClick
import net.ccbluex.gratefuldead.utils.client.onHover
import net.ccbluex.gratefuldead.utils.client.regular
import net.ccbluex.gratefuldead.utils.client.variable
import net.ccbluex.gratefuldead.utils.kotlin.unmodifiable
import net.ccbluex.gratefuldead.utils.text.AsyncLoadingText
import net.ccbluex.gratefuldead.utils.text.PlainText
import net.ccbluex.gratefuldead.utils.text.asPlainText
import net.ccbluex.gratefuldead.utils.text.plus
import net.ccbluex.gratefuldead.utils.text.textOf
import net.minecraft.ChatFormatting
import net.minecraft.SharedConstants
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.util.Util
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * LocalConfig Command
 *
 * Allows you to load, list, and create local configurations.
 */
object CommandLocalConfig : CommandRegistrar {
    @Suppress("detekt:LongMethod")
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("localconfig") {
            literal("load") {
                argument(
                    "name",
                    ClientStringArgumentType.word(),
                    suggestions(Util.nonCriticalIoPool()) {
                        ConfigSystem.userConfigsFolder.listFiles()?.map { it.nameWithoutExtension }
                    },
                ) { name ->
                    optional(
                        "modules",
                        MultiSelectArgumentType(
                            "Module",
                            ModuleManager,
                            predicate = { true },
                            nameOf = ClientModule::name
                        ),
                        default = null,
                    ) { modules ->
                        exec { ctx ->
                            loadConfig(ctx.get(name), ctx.get(modules) ?: emptySet())
                        }
                    }
                }
            }
            pagedList(
                pageSize = 8,
                header = {
                    highlight("Local Configs:")
                },
                items = {
                    ConfigSystem.userConfigsFolder.listFiles { _, name ->
                        name.endsWith(".json", ignoreCase = true)
                    }.unmodifiable()
                },
                eachRow = { _, file ->
                    val settingName = file.name.removeSuffix(".json")

                    val lastModified = Instant.ofEpochMilli(file.lastModified())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                        .format(AutoConfigMetadata.FORMATTER)

                    textOf(
                        "\u2B25 ".asPlainText(ChatFormatting.BLUE),
                        variable(file.name)
                            .onClick(
                                ClickEvent.SuggestCommand(
                                    CommandManager.GlobalSettings.prefix + "localconfig load $settingName"
                                )
                            )
                            .onHover(HoverEvent.ShowText(hoverText(file, settingName))),
                        regular(" ($lastModified)"),
                    )
                }
            )
            literal("browse") {
                exec {
                    browseConfigs()
                }
            }
            literal("save", aliases = listOf("create")) {
                argument(
                    "name",
                    ClientStringArgumentType.word(),
                    suggestions(Util.nonCriticalIoPool()) {
                        ConfigSystem.userConfigsFolder.listFiles()?.map { it.nameWithoutExtension }
                    },
                ) { name ->
                    optional("overwrite", BooleanArgumentType("overwrite"), default = null) { overwrite ->
                        optional(
                            "include",
                            MultiTaggedArgumentType("include", listOf("binds", "hidden", "render", "fun")) { it },
                            default = null,
                        ) { include ->
                            exec { ctx ->
                                saveConfig(
                                    ctx.get(name),
                                    ctx.get(overwrite) == true,
                                    ctx.get(include) ?: emptyList(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hoverText(file: File, settingName: String) =
        textOf(
            "Click to load ".asPlainText(ChatFormatting.GRAY),
            settingName.asPlainText(Style.EMPTY + ChatFormatting.AQUA + ChatFormatting.BOLD),
            PlainText.NEW_LINE,
            AsyncLoadingText(
                ioScope.async {
                    file.bufferedReader().use { r ->
                        publicGson.fromJson(r, AutoConfigMetadata::class.java)
                    }.asText()
                }
            )
        )

    private fun CmdI18n.saveConfig(name: String, overwrite: Boolean, include: List<String>): Int {
        if (name.isBlank() || name.indexOfAny(SharedConstants.ILLEGAL_FILE_CHARACTERS) != -1) {
            throw CommandException(t("save.invalidFileName", variable(name)))
        }

        val inclusions = enumSetOf<OptionalInclusion>()
        if (include.contains("render")) inclusions.add(OptionalInclusion.RENDER)
        if (include.contains("fun")) inclusions.add(OptionalInclusion.FUN)

        val includeConfiguration = IncludeConfiguration(
            includeBinds = include.contains("binds"),
            includeHidden = include.contains("hidden"),
            optionalInclusions = inclusions,
        )

        val file = ConfigSystem.userConfigsFolder.resolve("$name.json")
        try {
            if (file.exists()) {
                if (overwrite) {
                    file.delete()
                } else {
                    chat(markAsError(t("save.alreadyExists", variable(name))))
                    return 1
                }
            }

            file.createNewFile()
            serializeAutoConfig(
                file.bufferedWriter(),
                includeConfiguration,
                marketplaceItemId = ConfigTracker.trackedItemId
            )
            chat(regular(t("save.created", variable(name))))
        } catch (e: Exception) {
            chat(regular(t("save.failedToCreate", variable(name))))
            logger.error("Failed to create local config '$name'", e)
        }
        return 1
    }

    private fun CmdI18n.browseConfigs(): Int {
        Blaze3D.openPath(ConfigSystem.userConfigsFolder.toPath())
        chat(regular(t("browse.browse", clickablePath(ConfigSystem.userConfigsFolder))))
        return 1
    }

    private fun CmdI18n.loadConfig(name: String, modules: Set<ClientModule>): Int {
        ConfigSystem.userConfigsFolder.resolve("$name.json").runCatching {
            if (!exists()) {
                chat(regular(t("load.notFound", variable(name))))
                return 1
            }

            val config = bufferedReader().use { r -> publicGson.newJsonReader(r).parseTree().asJsonObject }
            AutoConfig.withLoading {
                AutoConfig.loadAutoConfig(config, modules)
            }
            config.int("marketplaceItemId")
        }.onFailure { error ->
            logger.error("Failed to load config $name", error)
            chat(markAsError(t("load.failedToLoad", variable(name))))
        }.onSuccess { origin ->
            chat(regular(t("load.loaded", variable(name))))
            ConfigTracker.loadedLocal(name, origin, partial = modules.isNotEmpty())?.let { untracked ->
                chat(regular(t("load.untracked", variable(untracked))))
            }
        }
        return 1
    }

}

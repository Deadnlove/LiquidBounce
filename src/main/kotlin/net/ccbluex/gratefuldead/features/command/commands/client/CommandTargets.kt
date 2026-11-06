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

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.gratefuldead.config.types.list.MultiChoiceListValue
import net.ccbluex.gratefuldead.features.command.CommandRegistrar
import net.ccbluex.gratefuldead.features.command.arguments.TaggedArgumentType
import net.ccbluex.gratefuldead.features.command.brigadier.ClientCommandSource
import net.ccbluex.gratefuldead.features.command.brigadier.CmdLiteralScope
import net.ccbluex.gratefuldead.features.command.brigadier.get
import net.ccbluex.gratefuldead.features.command.brigadier.register
import net.ccbluex.gratefuldead.features.global.GlobalSettingsTarget
import net.ccbluex.gratefuldead.features.module.modules.render.ModuleClickGui
import net.ccbluex.gratefuldead.lang.translation
import net.ccbluex.gratefuldead.utils.client.MessageMetadata
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.regular
import net.ccbluex.gratefuldead.utils.combat.Targets

/**
 * Enemy Command
 *
 * Provides subcommands for enemy configuration.
 */
object CommandTargets : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("targets", aliases = listOf("target", "enemies", "enemy")) {
            literal("combat") {
                targetsChoice("category", GlobalSettingsTarget.combatChoices, "combat")
            }
            literal("visual") {
                targetsChoice("category", GlobalSettingsTarget.visualChoices, "visual")
            }
        }
    }

    private fun CmdLiteralScope.targetsChoice(
        name: String,
        targets: MultiChoiceListValue<Targets>,
        subcommand: String,
    ) {
        argument(name, TaggedArgumentType<Targets>(name) { it in targets.choices }) { category ->
            exec { ctx ->
                val entry = ctx.get(category)

                val state = targets.toggle(entry)
                val localizedState = if (state) "enabled" else "disabled"
                chat(
                    regular(
                        translation(
                            "gratefuldead.command.targets.$subcommand.$localizedState",
                            entry.name.lowercase().replaceFirstChar { it.uppercase() }
                        )
                    ),
                    metadata = MessageMetadata(id = "CTargets#info")
                )

                ModuleClickGui.sync()
                1
            }
        }
    }

}

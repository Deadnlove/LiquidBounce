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
import net.ccbluex.gratefuldead.features.command.CommandRegistrar
import net.ccbluex.gratefuldead.features.command.arguments.ModuleArgumentType
import net.ccbluex.gratefuldead.features.command.brigadier.ClientCommandSource
import net.ccbluex.gratefuldead.features.command.brigadier.get
import net.ccbluex.gratefuldead.features.command.brigadier.register
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.utils.client.MessageMetadata
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.regular
import net.ccbluex.gratefuldead.utils.client.variable

/**
 * Toggle Command
 *
 * Allows you to enable or disable a specific module.
 */
object CommandToggle : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("toggle", aliases = listOf("t")) {
            argument("module", ModuleArgumentType("module")) { module ->
                exec { ctx ->
                    val toggledModule: ClientModule = ctx.get(module)

                    val isEnabled = !toggledModule.enabled
                    toggledModule.enabled = isEnabled
                    chat(
                        regular(
                            t("moduleToggled",
                                variable(toggledModule.name),
                                variable(
                                    if (isEnabled) {
                                        t("enabled")
                                    } else {
                                        t("disabled")
                                    }
                                )
                            )
                        ),
                        metadata = MessageMetadata(id = "CToggle#success${toggledModule.name}")
                    )
                    1
                }
            }
        }
    }

}

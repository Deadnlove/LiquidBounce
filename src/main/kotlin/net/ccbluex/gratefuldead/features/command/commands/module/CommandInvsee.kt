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
package net.ccbluex.gratefuldead.features.command.commands.module

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.gratefuldead.features.command.CommandException
import net.ccbluex.gratefuldead.features.command.CommandRegistrar
import net.ccbluex.gratefuldead.features.command.arguments.PlayerInfoArgumentType
import net.ccbluex.gratefuldead.features.command.brigadier.ClientCommandSource
import net.ccbluex.gratefuldead.features.command.brigadier.get
import net.ccbluex.gratefuldead.features.command.brigadier.register
import net.ccbluex.gratefuldead.features.module.modules.misc.ModuleInventoryTracker
import net.ccbluex.gratefuldead.utils.client.mc
import net.ccbluex.gratefuldead.utils.client.world
import net.ccbluex.gratefuldead.utils.inventory.ViewedInventoryScreen
import java.util.UUID

/**
 * Command Invsee
 *
 * Lets you view another player's inventory.
 *
 * Module: [ModuleInventoryTracker]
 */
object CommandInvsee : CommandRegistrar {
    var viewedPlayer: UUID? = null

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("invsee") {
            requires { it.isIngame }
            argument("name", PlayerInfoArgumentType) { name ->
                exec { ctx ->
                    val playerInfo = ctx.get(name)
                    val inputName = playerInfo.profile.name
                    val playerID = playerInfo.profile.id
                    val player = { world.getPlayerByUUID(playerID) ?: ModuleInventoryTracker.playerMap[playerID] }

                    if (player() == null) {
                        throw CommandException(t("playerNotFound", inputName))
                    }

                    mc.schedule {
                        mc.gui.setScreen(ViewedInventoryScreen(player))
                    }

                    viewedPlayer = playerID
                    1
                }
            }
        }
    }

}

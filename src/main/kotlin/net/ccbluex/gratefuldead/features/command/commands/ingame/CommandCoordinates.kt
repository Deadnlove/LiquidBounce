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
package net.ccbluex.gratefuldead.features.command.commands.ingame

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.gratefuldead.features.command.CommandRegistrar
import net.ccbluex.gratefuldead.features.command.arguments.ClientStringArgumentType
import net.ccbluex.gratefuldead.features.command.brigadier.ClientCommandSource
import net.ccbluex.gratefuldead.features.command.brigadier.get
import net.ccbluex.gratefuldead.features.command.brigadier.onlinePlayers
import net.ccbluex.gratefuldead.features.command.brigadier.register
import net.ccbluex.gratefuldead.utils.client.MessageMetadata
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.mc
import net.ccbluex.gratefuldead.utils.client.network
import net.ccbluex.gratefuldead.utils.client.player
import net.ccbluex.gratefuldead.utils.client.world
import net.ccbluex.gratefuldead.utils.text.asPlainText
import net.minecraft.ChatFormatting
import org.apache.commons.lang3.StringUtils

/**
 * Coordinates Command
 *
 * Copies your coordinates to your clipboard.
 */
object CommandCoordinates : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("coordinates", aliases = listOf("position", "coords")) {
            requires { it.isIngame }

            literal("whisper") {
                argument("name", ClientStringArgumentType.word(), onlinePlayers()) { name ->
                    exec { ctx ->
                        network.sendCommand("msg ${ctx.get(name)} ${getCoordinates(fancy = true)}")
                        1
                    }
                }
            }
            literal("copy") {
                exec {
                    mc.keyboardHandler.clipboard = getCoordinates()
                    chat(
                        t("copy.success"),
                        metadata = MessageMetadata(id = "Ccopy#info")
                    )
                    1
                }
            }
            literal("info") {
                exec {
                    chat(
                        getCoordinates().asPlainText(ChatFormatting.GRAY),
                        metadata = MessageMetadata(id = "Cinfo#info"),
                    )
                    1
                }
            }
        }
    }

    private fun getCoordinates(fancy: Boolean = false): String {
        val pos = player.blockPosition()
        val dimension = StringUtils.capitalize(world.dimension().identifier().path)
        val start = if (fancy) "My coordinates are: " else ""
        return start +
            "x: ${pos.x}, y: ${pos.y}, z: ${pos.z} " +
            "in the $dimension"
    }

}

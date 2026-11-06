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
package net.ccbluex.gratefuldead.features.command.commands.client.client

import net.ccbluex.gratefuldead.GratefulDead
import net.ccbluex.gratefuldead.features.command.brigadier.CmdLiteralScope
import net.ccbluex.gratefuldead.utils.client.MessageMetadata
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.regular
import net.ccbluex.gratefuldead.utils.client.variable

object CommandClientInfoSubcommand {
    fun CmdLiteralScope.info() {
        literal("info") {
            exec {
                chat(
                    regular(t("info.clientName", variable(GratefulDead.CLIENT_DISPLAY_NAME))),
                    metadata = MessageMetadata(prefix = false)
                )
                chat(
                    regular(t("info.clientVersion", variable(GratefulDead.clientVersion))),
                    metadata = MessageMetadata(prefix = false)
                )
                chat(
                    regular(t("info.clientAuthor", variable(GratefulDead.CLIENT_AUTHOR))),
                    metadata = MessageMetadata(prefix = false)
                )
                1
            }
        }
    }
}

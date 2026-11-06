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
package net.ccbluex.gratefuldead.features.command.commands.client.marketplace

import net.ccbluex.gratefuldead.features.command.arguments.ClientStringArgumentType
import net.ccbluex.gratefuldead.features.command.brigadier.CmdLiteralScope
import net.ccbluex.gratefuldead.features.command.brigadier.get
import net.ccbluex.gratefuldead.features.marketplace.MarketplaceManager
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.regular
import net.ccbluex.gratefuldead.utils.client.variable

/**
 * Unsubscribe from a marketplace item
 */
object MarketplaceUnsubscribeCommand {

    fun CmdLiteralScope.unsubscribe() {
        literal("unsubscribe") {
            argument("item", ClientStringArgumentType.string(), suggests = subscribedSuggestions) { input ->
                execSuspend { ctx ->
                    val item = subscribedItem(ctx.get(input))

                    MarketplaceManager.unsubscribe(item.id)
                    chat(regular(t("unsubscribe.success", variable(item.address))))
                }
            }
        }
    }

}

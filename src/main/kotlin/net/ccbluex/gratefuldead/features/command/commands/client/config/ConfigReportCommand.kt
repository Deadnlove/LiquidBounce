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
package net.ccbluex.gratefuldead.features.command.commands.client.config

import net.ccbluex.gratefuldead.GratefulDead
import net.ccbluex.gratefuldead.api.services.marketplace.MarketplaceApi
import net.ccbluex.gratefuldead.features.command.CommandException
import net.ccbluex.gratefuldead.features.command.arguments.ClientStringArgumentType
import net.ccbluex.gratefuldead.features.command.brigadier.CmdI18n
import net.ccbluex.gratefuldead.features.command.brigadier.CmdLiteralScope
import net.ccbluex.gratefuldead.features.command.brigadier.get
import net.ccbluex.gratefuldead.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.mc
import net.ccbluex.gratefuldead.utils.client.regular
import net.ccbluex.gratefuldead.utils.client.variable
import net.ccbluex.gratefuldead.utils.text.dropPort

/**
 * Tells other players whether a config works, by default the tracked one
 */
object ConfigReportCommand {

    fun CmdLiteralScope.report() {
        literal("report") {
            for (works in listOf(true, false)) {
                literal(if (works) "works" else "broken") {
                    optional(
                        "config",
                        ClientStringArgumentType.string(),
                        default = null,
                        suggests = configSuggestions
                    ) { config ->
                        execSuspend { ctx -> report(works, ctx.get(config)) }
                    }
                }
            }
        }
    }

    private suspend fun CmdI18n.report(works: Boolean, input: String?) {
        val (itemId, revisionId, name) = if (input == null) {
            requireTracked()
            Triple(ConfigTracker.itemId, ConfigTracker.revisionId, ConfigTracker.itemName)
        } else {
            val item = resolveConfig(input)
            val revisionId = item.liveRevisionId ?: throw CommandException(t("load.noRevision", variable(item.name)))
            Triple(item.id, revisionId, item.name)
        }

        request {
            MarketplaceApi.putConfigReport(
                session(),
                itemId,
                revisionId,
                works,
                GratefulDead.clientVersion,
                mc.currentServer?.ip?.dropPort()
            )
        }
        chat(regular(t("report.reported", variable(name))))
    }

}

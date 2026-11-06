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
import com.mojang.brigadier.tree.LiteralCommandNode
import net.ccbluex.gratefuldead.features.command.CommandManager
import net.ccbluex.gratefuldead.features.command.CommandRegistrar
import net.ccbluex.gratefuldead.features.command.brigadier.ClientCommandSource
import net.ccbluex.gratefuldead.features.command.brigadier.register
import net.ccbluex.gratefuldead.features.command.preset.pagedQuery
import net.ccbluex.gratefuldead.lang.translation
import net.ccbluex.gratefuldead.utils.client.bold
import net.ccbluex.gratefuldead.utils.client.onClick
import net.ccbluex.gratefuldead.utils.client.onHover
import net.ccbluex.gratefuldead.utils.client.regular
import net.ccbluex.gratefuldead.utils.client.withColor
import net.ccbluex.gratefuldead.utils.text.asText
import net.ccbluex.gratefuldead.utils.text.buildText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.network.chat.HoverEvent

/**
 * Help Command
 *
 * Provides a help page for displaying other commands.
 */
object CommandHelp : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("help") {
            pagedQuery(
                header = {
                    t("help").withColor(ChatFormatting.RED).bold(true)
                },
                items = {
                    CommandManager.mainCommandNodes
                },
                eachRow = { _, node ->
                    buildRow(node)
                }
            )
        }
    }

    private fun buildRow(node: LiteralCommandNode<ClientCommandSource>): Component {
        val commandName = node.name
        val commandStart = CommandManager.GlobalSettings.prefix + commandName
        return "\u2B25 ".asText()
            .withStyle(ChatFormatting.BLUE)
            .onHover(
                HoverEvent.ShowText(
                    translation("gratefuldead.command.$commandName.description")
                )
            )
            .append(
                commandStart.asText()
                    .withStyle(ChatFormatting.GRAY)
                    .onClick(ClickEvent.SuggestCommand(commandStart))
            )
            .append(buildAliasesText(node))
    }

    /**
     * Alias nodes are registered as redirecting literals that share the main node's subtree.
     */
    private fun buildAliasesText(mainNode: LiteralCommandNode<ClientCommandSource>): Component = buildText {
        val aliases = CommandManager.rootCommandNodes
            .filter { it.redirect === mainNode }
            .map { it.name }

        aliases.forEach { alias ->
            this += ComponentUtils.DEFAULT_SEPARATOR
            this += regular(alias).withStyle(ChatFormatting.GRAY)
                .onClick(ClickEvent.SuggestCommand(CommandManager.GlobalSettings.prefix + alias))
        }
    }

}

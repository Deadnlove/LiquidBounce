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

import net.ccbluex.gratefuldead.features.command.brigadier.CmdLiteralScope
import net.ccbluex.gratefuldead.integration.screen.CustomScreenType
import net.ccbluex.gratefuldead.integration.screen.ScreenManager
import net.ccbluex.gratefuldead.integration.theme.ThemeManager
import net.ccbluex.gratefuldead.utils.client.MessageMetadata
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.copyable
import net.ccbluex.gratefuldead.utils.client.italic
import net.ccbluex.gratefuldead.utils.client.onClick
import net.ccbluex.gratefuldead.utils.client.onHover
import net.ccbluex.gratefuldead.utils.client.regular
import net.ccbluex.gratefuldead.utils.client.underline
import net.ccbluex.gratefuldead.utils.client.variable
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.ComponentUtils.DEFAULT_SEPARATOR
import net.minecraft.network.chat.HoverEvent
import java.net.URI

object CommandClientIntegrationSubcommand {
    fun CmdLiteralScope.integration() {
        literal("integration") {
            literal("reset") {
                exec {
                    chat(regular("Resetting client browser..."))
                    ScreenManager.update()
                    1
                }
            }
            literal("menu") {
                exec {
                    menu()
                }
            }
            // "url" behaves as an alias of "menu"
            literal("url") {
                exec {
                    menu()
                }
            }
        }
    }

    private fun menu(): Int {
        chat(variable("Client Integration"))
        val baseUrl = ThemeManager.getScreenLocation().url

        chat(
            regular("Base URL: ")
                .append(
                    variable(baseUrl)
                        .underline(true)
                        .onClick(ClickEvent.OpenUrl(URI(baseUrl)))
                        .onHover(
                            HoverEvent.ShowText(
                                regular("Click to open the integration URL in your browser.")
                            )
                        )
                ),
            metadata = MessageMetadata(
                prefix = false
            )
        )

        chat(metadata = MessageMetadata(prefix = false))
        chat(regular("Integration Menu:"))
        for (screenType in CustomScreenType.entries) {
            val url = runCatching {
                ThemeManager.getScreenLocation(screenType, true)
            }.getOrNull()?.url ?: continue
            val upperFirstName = screenType.routeName.replaceFirstChar { it.uppercase() }

            chat(
                regular("-> $upperFirstName (")
                    .append(
                        variable("Browser")
                            .underline(true)
                            .onClick(ClickEvent.OpenUrl(URI(url)))
                            .onHover(
                                HoverEvent.ShowText(
                                    regular("Click to open the URL in your browser.")
                                )
                            )
                    )
                    .append(DEFAULT_SEPARATOR)
                    .append(
                        variable("Clipboard")
                            .copyable(
                                copyContent = url, hover = HoverEvent.ShowText(
                                    regular("Click to copy the URL to your clipboard.")
                                )
                            )
                            .underline(true)
                    )
                    .append(regular(")")),
                metadata = MessageMetadata(
                    prefix = false
                )
            )
        }

        chat(variable("Hint: You can also access the integration from another device.").italic(true))
        return 1
    }
}

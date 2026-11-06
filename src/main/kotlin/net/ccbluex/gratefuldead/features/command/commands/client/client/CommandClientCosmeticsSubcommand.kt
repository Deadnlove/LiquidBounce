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

import kotlinx.coroutines.suspendCancellableCoroutine
import net.ccbluex.gratefuldead.features.command.brigadier.CmdLiteralScope
import net.ccbluex.gratefuldead.features.cosmetic.ClientAccountManager
import net.ccbluex.gratefuldead.features.cosmetic.CosmeticService
import net.ccbluex.gratefuldead.utils.client.browseUrl
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.regular
import kotlin.coroutines.resume

object CommandClientCosmeticsSubcommand {
    fun CmdLiteralScope.cosmetics() {
        literal("cosmetics") {
            literal("refresh") {
                execSuspend {
                    refresh()
                }
            }
            literal("manage") {
                exec {
                    browseUrl("https://user.liquidbounce.net/cosmetics")
                    1
                }
            }
        }
    }

    private suspend fun refresh() {
        chat(
            regular(
                "Refreshing cosmetics..."
            )
        )
        CosmeticService.carriersCosmetics.clear()
        ClientAccountManager.clientAccount.cosmetics = null

        suspendCancellableCoroutine { continuation ->
            CosmeticService.refreshCarriers(true) {
                chat(
                    regular(
                        "Cosmetic System has been refreshed."
                    )
                )
                continuation.resume(Unit)
            }
        }
    }
}

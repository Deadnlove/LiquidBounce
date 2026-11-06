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

package net.ccbluex.gratefuldead.features.command.preset

import net.ccbluex.gratefuldead.api.models.auth.ClientAccount
import net.ccbluex.gratefuldead.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.gratefuldead.features.command.CommandException
import net.ccbluex.gratefuldead.features.cosmetic.ClientAccountManager
import net.ccbluex.gratefuldead.lang.translation

internal fun ClientAccountManager.accountOrException(): ClientAccount {
    val clientAccount = clientAccount
    if (clientAccount == EMPTY_ACCOUNT) {
        throw CommandException(translation("gratefuldead.command.marketplace.error.notLoggedIn"))
    }
    return clientAccount
}

/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
@file:Suppress("TooManyFunctions")

package net.ccbluex.gratefuldead.integration.interop.protocol.rest.v1.client

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import net.ccbluex.gratefuldead.api.models.auth.ClientAccount
import net.ccbluex.gratefuldead.api.services.auth.OAuthClient.startAuth
import net.ccbluex.gratefuldead.config.ConfigSystem
import net.ccbluex.gratefuldead.config.gson.interopGson
import net.ccbluex.gratefuldead.event.EventManager
import net.ccbluex.gratefuldead.event.events.UserLoggedInEvent
import net.ccbluex.gratefuldead.event.events.UserLoggedOutEvent
import net.ccbluex.gratefuldead.features.cosmetic.ClientAccountManager
import net.ccbluex.gratefuldead.integration.interop.badRequest
import net.ccbluex.gratefuldead.integration.interop.unauthorized
import net.ccbluex.gratefuldead.utils.client.browseUrl

// GET /api/v1/client/user
private fun Route.getUser() = get {
    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount == ClientAccount.EMPTY_ACCOUNT) {
        call.unauthorized("Not logged in")
    }

    val userInformation = clientAccount.userInformation ?: run {
        clientAccount.updateInfo()
        clientAccount.userInformation
    }

    call.respond(interopGson.toJsonTree(userInformation))
}

// POST /api/v1/client/user/login
private fun Route.loginUser() = post("/login") {
    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount != ClientAccount.EMPTY_ACCOUNT) {
        call.badRequest("Already logged in")
    }

    val account = startAuth(::browseUrl).apply {
        updateInfo()
    }
    ClientAccountManager.clientAccount = account
    ConfigSystem.store(ClientAccountManager)
    EventManager.callEvent(UserLoggedInEvent)

    call.respond(interopGson.toJsonTree(account.userInformation))
}

// POST /api/v1/client/user/logout
private fun Route.logoutUser() = post("/logout") {
    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount == ClientAccount.EMPTY_ACCOUNT) {
        call.badRequest("Not logged in")
    }

    ClientAccountManager.clientAccount = ClientAccount.EMPTY_ACCOUNT
    ConfigSystem.store(ClientAccountManager)
    EventManager.callEvent(UserLoggedOutEvent)
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

internal fun Route.userRoutes() = route("/user") {
    getUser()
    loginUser()
    logoutUser()
}

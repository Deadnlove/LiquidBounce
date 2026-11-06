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
package net.ccbluex.gratefuldead.integration.interop

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.webSocket
import net.ccbluex.gratefuldead.GratefulDead
import net.ccbluex.gratefuldead.config.gson.interopGson
import net.ccbluex.gratefuldead.features.marketplace.MarketplaceManager
import net.ccbluex.gratefuldead.integration.interop.middleware.AuthPlugin
import net.ccbluex.gratefuldead.integration.interop.middleware.isWebSocketAuthenticated
import net.ccbluex.gratefuldead.integration.interop.protocol.event.SocketEventListener
import net.ccbluex.gratefuldead.integration.interop.protocol.event.WebSocketSessionManager
import net.ccbluex.gratefuldead.integration.interop.protocol.rest.v1.registerInteropFunctions
import net.ccbluex.gratefuldead.integration.interop.protocol.rest.v1.respondJsonWriter
import net.ccbluex.gratefuldead.integration.theme.Theme
import net.ccbluex.gratefuldead.integration.theme.ThemeManager
import net.ccbluex.gratefuldead.utils.client.env
import net.ccbluex.gratefuldead.utils.client.error.ErrorHandler
import net.ccbluex.gratefuldead.utils.client.logger
import org.apache.commons.lang3.RandomStringUtils
import java.net.BindException
import java.net.ServerSocket
import kotlin.time.Duration.Companion.seconds

/**
 * A client server implementation.
 *
 * Allows the browser to communicate with the client. (e.g. for UIs)
 */
object ClientInteropServer {

    private var server: EmbeddedServer<*, *>? = null

    val isSkipping = env("GD_INTEROP_SKIP", "net.ccbluex.gratefuldead.interop.skip")?.toBoolean()
        ?: false

    var PORT = env("GD_INTEROP_PORT", "net.ccbluex.gratefuldead.interop.port")?.toIntOrNull()
        ?: ServerSocket(0).use { socket -> socket.localPort }
    val AUTH_CODE: String = env("GD_INTEROP_AUTH_CODE", "net.ccbluex.gratefuldead.interop.authCode")
        ?: RandomStringUtils.secure().nextAlphanumeric(16)

    val url get() = "http://127.0.0.1:$PORT"

    suspend fun start() {
        if (isSkipping) {
            logger.warn("Environment variable 'GD_INTEROP_SKIP' is set to 'true'.")
            return
        }

        val authCode = AUTH_CODE

        this.PORT = startServer(this.PORT, authCode)

        // Register events with @WebSocketEvent annotation
        SocketEventListener.registerAll()
    }

    suspend fun stop() {
        server?.stopSuspend(gracePeriodMillis = 1000, timeoutMillis = 2000)
        server = null
    }

    private var attempt = 0

    private suspend fun startServer(port: Int, authCode: String): Int {
        return try {
            val engine = embeddedServer(CIO, host = "127.0.0.1", port = port) {
                install(StatusPages) {
                    exception<HttpStatusException> { call, cause ->
                        call.respond(cause.status, cause.body)
                    }
                }

                installGson(interopGson)

                installCors()

                install(Compression) {
                    default()
                }

                install(AuthPlugin) {
                    this.authCode = authCode
                }

                install(WebSockets) {
                    this.pingPeriod = 15.seconds
                }

                routing {
                    // WebSocket endpoint
                    webSocket("/") {
                        val authenticated = isWebSocketAuthenticated(this, authCode)
                            || ThemeManager.isThemeExternal
                        if (!authenticated) {
                            logger.warn("[Interop] Unauthenticated web socket upgrade request")
                            return@webSocket
                        }

                        WebSocketSessionManager.add(this)

                        try {
                            this.closeReason.await()
                        } finally {
                            WebSocketSessionManager.remove(this)
                        }
                    }

                    rootResponse()

                    registerInteropFunctions()

                    // Static file serving
                    staticFiles("/local", ThemeManager.themesFolder)
                    staticFiles("/marketplace", MarketplaceManager.marketplaceRoot)

                    singlePageApplication {
                        applicationRoute = "/${Theme.Origin.RESOURCE.tag}/${GratefulDead.CLIENT_NAME.lowercase()}"
                        filesPath = "resources/gratefuldead/themes/${GratefulDead.CLIENT_NAME.lowercase()}"
                        useResources = true
                    }
                }
            }

            engine.start(wait = false)
            this.server = engine

            engine.engine.resolvedConnectors().first().port
        } catch (bindException: BindException) {
            if (attempt >= 5) {
                ErrorHandler.fatal(bindException, additionalMessage = "Bind interop server")
            }

            attempt++
            logger.error("Failed to bind to port $port. Falling back to random port.")
            startServer((15001..17000).random(), authCode)
        } catch (exception: Exception) {
            ErrorHandler.fatal(exception, additionalMessage = "Start interop server")
        }
    }

    private fun Route.rootResponse() = get("/") {
        call.respondJsonWriter {
            beginObject()
            name("name").value(GratefulDead.CLIENT_DISPLAY_NAME)
            name("version").value(GratefulDead.clientVersion)
            name("author").value(GratefulDead.CLIENT_AUTHOR)
            endObject()
        }
    }

}

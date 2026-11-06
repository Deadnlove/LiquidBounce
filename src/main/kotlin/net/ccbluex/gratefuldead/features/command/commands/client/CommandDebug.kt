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

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.gratefuldead.GratefulDead
import net.ccbluex.gratefuldead.api.core.HttpClient
import net.ccbluex.gratefuldead.api.core.HttpMethod
import net.ccbluex.gratefuldead.api.core.asForm
import net.ccbluex.gratefuldead.config.autoconfig.AutoConfig.serializeAutoConfig
import net.ccbluex.gratefuldead.config.gson.publicGson
import net.ccbluex.gratefuldead.config.gson.serializer.minecraft.accountType
import net.ccbluex.gratefuldead.config.gson.util.toJsonArray
import net.ccbluex.gratefuldead.features.command.CommandRegistrar
import net.ccbluex.gratefuldead.features.command.brigadier.ClientCommandSource
import net.ccbluex.gratefuldead.features.command.brigadier.register
import net.ccbluex.gratefuldead.features.global.GlobalSettingsTarget
import net.ccbluex.gratefuldead.features.module.ModuleManager
import net.ccbluex.gratefuldead.lang.LanguageManager
import net.ccbluex.gratefuldead.features.addon.AddonManager
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.mc
import net.ccbluex.gratefuldead.utils.client.usesViaFabricPlus
import net.ccbluex.gratefuldead.utils.text.asPlainText
import net.ccbluex.gratefuldead.utils.text.plus
import net.minecraft.ChatFormatting
import net.minecraft.SharedConstants
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Style
import java.net.URI
import java.util.EnumSet

/**
 * Debug Command to collect information about the client
 * in order to help developers to fix bugs or help users
 * with their issues.
 *
 * This command will create a JSON file with all the information
 * and send it to the CCBlueX Paste API.
 */
object CommandDebug : CommandRegistrar {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    override fun register(dispatcher: com.mojang.brigadier.CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("debug") {
            execSuspend {
                chat("§7Collecting debug information...")

                val buffer = okio.Buffer()

                buffer.outputStream().writer().use {
                    serializeAutoConfig(it)
                }
                val autoConfig = buffer.readUtf8()
                val autoConfigPaste = uploadToPaste(autoConfig)
                buffer.clear()

                val debugJson = createDebugJson(autoConfigPaste)
                buffer.outputStream().writer().use {
                    gson.toJson(debugJson, it)
                }
                val paste = uploadToPaste(buffer.readUtf8())
                buffer.clear()

                chat(
                    "Debug information has been uploaded to: ".asPlainText(ChatFormatting.GREEN),
                    paste.asPlainText(Style.EMPTY + ChatFormatting.YELLOW + ClickEvent.OpenUrl(URI(paste))),
                )
            }
        }
    }

    @Suppress("LongMethod")
    private fun createDebugJson(
        autoConfigPaste: String
    ) = JsonObject().apply {
        add("client", JsonObject().apply {
            addProperty("name", GratefulDead.CLIENT_DISPLAY_NAME)
            addProperty("version", GratefulDead.clientVersion)
            addProperty("commit", GratefulDead.clientCommit)
            addProperty("branch", GratefulDead.clientBranch)
            addProperty("development", GratefulDead.IN_DEVELOPMENT)
            addProperty("usesViaFabricPlus", usesViaFabricPlus)
        })

        add("minecraft", JsonObject().apply {
            addProperty("version", SharedConstants.getCurrentVersion().name())
            addProperty("protocol", SharedConstants.getProtocolVersion())
        })

        add("java", JsonObject().apply {
            addProperty("version", System.getProperty("java.version"))
            addProperty("vendor", System.getProperty("java.vendor"))
        })

        add("os", JsonObject().apply {
            addProperty("name", System.getProperty("os.name"))
            addProperty("version", System.getProperty("os.version"))
            addProperty("architecture", System.getProperty("os.arch"))
        })

        add("user", JsonObject().apply {
            addProperty("language", System.getProperty("user.language"))
            addProperty("country", System.getProperty("user.country"))
            addProperty("timezone", System.getProperty("user.timezone"))
        })

        add("profile", JsonObject().apply {
            addProperty("name", mc.user.name)
            addProperty("uuid", mc.user.profileId.toString())
            addProperty("type", mc.user.accountType)
        })

        add("language", JsonObject().apply {
            addProperty("language", mc.languageManager.selected)
            addProperty("clientLanguage", LanguageManager.clientLanguage.tag)
        })

        add("server", JsonObject().apply {
            mc.currentServer?.let {
                addProperty("name", it.name)
                addProperty("address", it.ip)
                addProperty("protocol", it.protocol)
            }
        })

        addProperty("config", autoConfigPaste)

        add("activeModules", JsonArray().apply {
            ModuleManager.filter { it.running }.forEach { module ->
                add(JsonPrimitive(module.name))
            }
        })

        add("addons", JsonArray().apply {
            AddonManager.addons.forEach { addon ->
                add(JsonObject().apply {
                    addProperty("id", addon.id)
                    addProperty("name", addon.displayName)
                    addProperty("version", addon.version)
                    add("authors", addon.authors.toJsonArray())
                    addProperty("state", addon.state.name)
                    addProperty("modules", addon.registeredModules.size)
                })
            }
        })

        add("enemies", publicGson.toJsonTree(GlobalSettingsTarget.combat, EnumSet::class.javaObjectType))
    }

    /**
     * Uploads the given content to the CCBlueX Paste API
     * and returns the URL of the paste.
     */
    private suspend fun uploadToPaste(content: String): String = withContext(Dispatchers.IO) {
        val form = "content=$content"
        HttpClient.request("https://paste.ccbluex.net/api.php", HttpMethod.POST, body = form.asForm())
                .body.string()
    }

}

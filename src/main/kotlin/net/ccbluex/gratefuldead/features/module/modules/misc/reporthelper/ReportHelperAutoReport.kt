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

package net.ccbluex.gratefuldead.features.module.modules.misc.reporthelper

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import kotlinx.coroutines.Dispatchers
import net.ccbluex.gratefuldead.config.types.group.ToggleableValueGroup
import net.ccbluex.gratefuldead.event.events.ChatReceiveEvent
import net.ccbluex.gratefuldead.event.events.DisconnectEvent
import net.ccbluex.gratefuldead.event.events.SessionEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.event.sequenceHandler
import net.ccbluex.gratefuldead.event.suspendHandler
import net.ccbluex.gratefuldead.event.waitTicks
import net.ccbluex.gratefuldead.features.misc.FriendManager
import kotlin.random.Random

internal object ReportHelperAutoReport : ToggleableValueGroup(ModuleReportHelper, "AutoReport", false) {

    private val delay by intRange("Delay", 1..3, 0..20, "ticks")
    private val chance by int("Chance", 100, 1..100, "%")
    private val pattern by text("CommandPattern", "report %s")

    private val reported: MutableSet<String> = ObjectRBTreeSet(String.CASE_INSENSITIVE_ORDER)

    @Suppress("unused")
    private val chatHandler = sequenceHandler<ChatReceiveEvent> { event ->
        val message = event.message

        val selfName = player.gameProfile.name
        if (message.contains(selfName)) {
            val another = world.players().firstNotNullOfOrNull { entity ->
                entity.gameProfile.name.takeIf { name ->
                    entity !== player && name != selfName && message.contains(name) && !FriendManager.isFriend(name)
                }
            } ?: return@sequenceHandler

            if (Random.nextInt(100) >= chance || !reported.add(another)) {
                return@sequenceHandler
            }

            waitTicks(delay.random())
            player.connection.sendCommand(pattern.format(another))
        }
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        reported.clear()
    }

    @Suppress("unused")
    private val sessionHandler = suspendHandler<SessionEvent>(Dispatchers.Main) {
        reported.clear()
    }

    override fun onDisabled() {
        reported.clear()
        super.onDisabled()
    }

}

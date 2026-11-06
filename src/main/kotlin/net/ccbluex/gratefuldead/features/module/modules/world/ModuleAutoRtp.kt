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
package net.ccbluex.gratefuldead.features.module.modules.world

import net.ccbluex.gratefuldead.event.events.GameTickEvent
import net.ccbluex.gratefuldead.event.events.NotificationEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.utils.client.notification
import net.ccbluex.gratefuldead.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE

/** Uses the server's RTP command; the server decides whether and where to teleport. */
object ModuleAutoRtp : ClientModule("AutoRTP", ModuleCategories.WORLD, disableOnQuit = true) {
    private val mode by enumChoice("Mode", AutoRtpMode.FAR_COORDINATES)
    private val command by text("Command", "/rtp")
    private val startDelay by int("StartDelay", 3, 0..60, "s")
    private val delay by int("Delay", 60, 1..3600, "s")
    private val scanDelay by int("ScanDelay", 10, 1..120, "s")
    private val targetDistance by int("TargetDistance", 1_000_000, 1_000..30_000_000, "m")
    private var cycle: AutoRtpCycle? = null
    private var finderPrepared = false
    private val now get() = System.nanoTime() / 1E9

    override fun onEnabled() {
        cycle = AutoRtpCycle(now)
        finderPrepared = false
    }

    override fun onDisabled() {
        cycle = null
        finderPrepared = false
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(READ_FINAL_STATE) {
        if (!prepareFinder()) return@handler
        if (mode == AutoRtpMode.FAR_COORDINATES && autoRtpReachedDistance(player.x, player.z, targetDistance)) {
            stop("Target distance from 0,0 reached; Auto RTP stopped.")
            return@handler
        }
        val current = now
        val timing = AutoRtpTiming(mode, startDelay.toDouble(), delay.toDouble(), scanDelay.toDouble())
        when (cycle?.step(current, ModuleBaseFinder.hasNearbyFinding, timing)) {
            AutoRtpAction.SEND -> sendRequest(current)
            AutoRtpAction.FOUND -> stop("Possible base found; Auto RTP stopped.")
            else -> Unit
        }
    }

    private fun prepareFinder(): Boolean {
        if (mode != AutoRtpMode.UNTIL_FOUND) {
            finderPrepared = false
            return true
        }
        if (!finderPrepared) {
            if (!ModuleBaseFinder.enabled) ModuleBaseFinder.enabled = true
            finderPrepared = true
        }
        if (ModuleBaseFinder.running) return true
        stop("Base Finder is disabled; Auto RTP stopped.")
        return false
    }

    private fun sendRequest(current: Double) {
        val request = autoRtpCommand(command)
        if (request == null) {
            stop("Enter a valid server command for Auto RTP.")
            return
        }
        cycle?.sent(current)
        network.sendCommand(request)
        if (mode == AutoRtpMode.ONCE) enabled = false
    }

    private fun stop(reason: String) {
        notification(name, reason, NotificationEvent.Severity.INFO)
        enabled = false
    }
}

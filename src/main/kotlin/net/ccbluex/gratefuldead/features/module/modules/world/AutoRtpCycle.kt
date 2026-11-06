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

import net.ccbluex.gratefuldead.config.types.list.Tagged

internal enum class AutoRtpMode(override val tag: String) : Tagged {
    ONCE("Once"), REPEAT("Repeat"), UNTIL_FOUND("UntilFound"), FAR_COORDINATES("FarCoordinates")
}

internal enum class AutoRtpAction { NONE, SEND, FOUND }

/** Monotonic time is supplied by the caller, allowing the timing policy to be tested independently. */
internal class AutoRtpCycle(private val started: Double) {
    private var lastRequest: Double? = null

    fun step(now: Double, found: Boolean, settings: AutoRtpTiming): AutoRtpAction {
        val last = lastRequest ?: return if (now - started >= settings.startDelay) {
            AutoRtpAction.SEND
        } else {
            AutoRtpAction.NONE
        }
        val elapsed = now - last
        if (settings.mode == AutoRtpMode.UNTIL_FOUND && elapsed >= settings.scanDelay && found) {
            return AutoRtpAction.FOUND
        }
        val delay = if (settings.mode == AutoRtpMode.UNTIL_FOUND) {
            maxOf(settings.interval, settings.scanDelay)
        } else {
            settings.interval
        }
        return if (elapsed >= delay) AutoRtpAction.SEND else AutoRtpAction.NONE
    }

    fun sent(now: Double) { lastRequest = now }
}

internal data class AutoRtpTiming(
    val mode: AutoRtpMode,
    val startDelay: Double,
    val interval: Double,
    val scanDelay: Double,
)

/** Returns only a single, nonempty command; arguments remain available for server-specific RTP plugins. */
internal fun autoRtpCommand(input: String): String? {
    val command = input.trim().removePrefix("/")
    if (command.isEmpty() || command.length > 255 || command.startsWith('/') || command.any(Char::isISOControl)) {
        return null
    }
    return command
}

/** Horizontal distance from world origin; doubles avoid integer overflow at the world border. */
internal fun autoRtpReachedDistance(x: Double, z: Double, target: Int): Boolean {
    val threshold = target.toDouble()
    return x * x + z * z >= threshold * threshold
}

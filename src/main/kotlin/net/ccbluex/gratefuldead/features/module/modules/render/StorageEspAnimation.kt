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

package net.ccbluex.gratefuldead.features.module.modules.render

import kotlin.math.exp
import net.ccbluex.gratefuldead.render.esp.EspVisualStyle

/** Exponential smoothing has the same response at 30, 60, and 144 Hz. */
internal fun storageSmoothing(deltaSeconds: Double): Double = 1.0 - exp(-12.0 * deltaSeconds.coerceIn(0.0, 0.25))

/** Retargeting preserves the current opacity, including when a target reappears during fade-out. */
internal class StorageEspTransition(private val duration: Double) {
    private var start = 0.0
    private var target = 0.0
    private var changedAt = 0.0

    init { require(duration > 0.0) }

    fun sample(now: Double): Double = start + (target - start) * EspVisualStyle.ease((now - changedAt) / duration)

    fun show(visible: Boolean, now: Double) {
        val next = if (visible) 1.0 else 0.0
        if (next == target) return
        start = sample(now)
        target = next
        changedAt = now
    }
}

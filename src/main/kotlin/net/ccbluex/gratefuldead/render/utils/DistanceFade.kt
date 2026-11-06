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

package net.ccbluex.gratefuldead.render.utils

/** Matches the relative-to-camera shaders, including equal endpoints (a hard cutoff). */
internal fun distanceFadeFactor(
    distance: Float,
    nearStart: Float,
    nearEnd: Float,
    farStart: Float,
    farEnd: Float,
): Float {
    fun ramp(start: Float, end: Float): Float {
        if (start == end) return if (distance >= end) 1F else 0F
        val t = ((distance - start) / (end - start)).coerceIn(0F, 1F)
        return t * t * (3F - 2F * t)
    }
    return ramp(nearStart, nearEnd) * (1F - ramp(farStart, farEnd))
}

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

import net.ccbluex.gratefuldead.render.esp.EspVisualStyle
import kotlin.math.floor

/** The marker illustrates a possible route; it does not observe item transfers. */
internal data class StorageEspRouteMarker(val progress: Double, val opacity: Double)

internal fun storageRouteMarker(seconds: Double, source: Long, target: Long): StorageEspRouteMarker {
    // A position-derived offset gives neighboring routes independent, stable phases.
    val mixed = source xor java.lang.Long.rotateLeft(target, 23)
    val offset = (mixed xor (mixed ushr 32)).toInt().toUInt().toDouble() / 4294967296.0
    val cycle = seconds / 2.5 + offset
    val progress = cycle - floor(cycle)
    val edge = minOf(progress, 1.0 - progress) / 0.18
    return StorageEspRouteMarker(progress, EspVisualStyle.ease(edge))
}

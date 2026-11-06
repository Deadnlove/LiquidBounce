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

package net.ccbluex.gratefuldead.render.esp

import net.minecraft.client.Minecraft

/** Shared visual tokens for every custom ESP renderer. World and GUI lengths use one scale conversion. */
internal object EspVisualStyle {
    const val CORNER_RADIUS = 0.12
    const val GUI_RADIUS = (CORNER_RADIUS * 32).toFloat()
    const val LABEL_PADDING = 4
    const val FADE_SECONDS = 0.25
    const val LINE_WIDTH = 1.5F
    val font get() = Minecraft.getInstance().font
    val lineWidth: Float get() = LINE_WIDTH * (Minecraft.getInstance().window.width / 1920F).coerceAtLeast(1F)

    fun ease(progress: Double): Double {
        val t = progress.coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }
}

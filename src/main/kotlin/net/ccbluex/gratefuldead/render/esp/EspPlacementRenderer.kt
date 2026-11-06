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

import net.ccbluex.gratefuldead.event.EventListener
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.utils.render.placement.PlacementRenderer

internal class EspPlacementRenderer(
    name: String,
    enabled: Boolean,
    module: EventListener,
    keep: Boolean = true,
    defaultColor: Color4b = Color4b.GREEN.alpha(90),
) : PlacementRenderer(name, enabled, module, keep = keep, defaultColor = defaultColor) {
    override val unifiedEspStyle = true
}

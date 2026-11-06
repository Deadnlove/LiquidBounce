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
package net.ccbluex.gratefuldead.features.module.modules.render.esp.modes

import net.ccbluex.gratefuldead.config.types.group.ToggleableValueGroup
import net.ccbluex.gratefuldead.event.events.OverlayRenderEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.modules.render.esp.ModuleESP.getColor
import net.ccbluex.gratefuldead.render.drawRoundedRect
import net.ccbluex.gratefuldead.render.esp.EspVisualStyle
import net.ccbluex.gratefuldead.render.drawVerticalLine
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.render.withPush
import net.ccbluex.gratefuldead.utils.entity.getActualHealth
import net.ccbluex.gratefuldead.utils.render.WorldToScreen

object Esp2DMode : EspMode.BoxBased("2D") {

    private object Outline : ToggleableValueGroup(this, "Outline", true) {
        val thickness by float("Thickness", 1f, 1f..9f, "px")
    }

    private object Corner : ToggleableValueGroup(this, "Corner", false) {
        val corner by float("Gap", 50f, 1f..100f, "%")
    }

    private object Border : ToggleableValueGroup(this, "Border", true) {
        val thickness by float("Thickness", 1f, 1f..9f, "px")
    }

    private val fill by boolean("Fill", true)

    object HealthBar : ToggleableValueGroup(this, "HealthBar", true) {
        val spacing by float("Spacing", 2f, 0f..32f, "px")
    }

    init {
        tree(Outline)
        tree(Corner)
        tree(Border)
        tree(HealthBar)
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        for ((entity, _, _, box) in collectPreparedBoxes(event.tickDelta)) {
            val rect = WorldToScreen.calculateScreenRect(box) ?: continue

            val color = getColor(entity)
            val baseColor = color.with(a = 50)
            val outlineColor = color.with(a = 255)
            val black = Color4b.BLACK

            val (minX, minY) = rect
            val rectWidth = rect.w
            val rectHeight = rect.h

            val guiScaleFactor = mc.window.guiScale.toFloat().coerceAtLeast(1f)
            val outlineThickness = Outline.thickness / guiScaleFactor
            val borderThickness = Border.thickness / guiScaleFactor

            with(event.context) {
                pose().withPush {
                    translate(minX, minY)

                    val radius = EspVisualStyle.GUI_RADIUS
                    if (fill) {
                        drawRoundedRect(0f, 0f, rectWidth, rectHeight, radius, fillColor = baseColor)
                    }
                    if (Outline.enabled) {
                        fun frame() {
                            if (Border.enabled) {
                                drawRoundedRect(0f, 0f, rectWidth, rectHeight, radius,
                                    outlineColor = black, outlineWidth = outlineThickness + 2 * borderThickness)
                            }
                            drawRoundedRect(0f, 0f, rectWidth, rectHeight, radius,
                                outlineColor = outlineColor, outlineWidth = outlineThickness)
                        }
                        if (Corner.enabled) {
                            val portion = (1f - Corner.corner / 100f).coerceIn(0f, 1f) / 2f
                            val cw = rectWidth * portion
                            val ch = rectHeight * portion
                            val margin = outlineThickness + borderThickness
                            for ((x, y) in listOf(0f to 0f, rectWidth - cw to 0f,
                                0f to rectHeight - ch, rectWidth - cw to rectHeight - ch)) {
                                enableScissor((x - margin).toInt(), (y - margin).toInt(),
                                    (x + cw + margin).toInt(), (y + ch + margin).toInt())
                                try {
                                    frame()
                                } finally {
                                    disableScissor()
                                }
                            }
                        } else {
                            frame()
                        }
                        if (Border.enabled) translate(-2 * borderThickness, 0f)
                    }

                    translate(-HealthBar.spacing / guiScaleFactor - outlineThickness, 0.0f)

                    if (HealthBar.enabled) {
                        val actualHealth = entity.getActualHealth()
                        val maxHealth = entity.maxHealth.coerceAtLeast(1f) // prevent division by zero
                        val healthPercentage = (actualHealth / maxHealth).coerceIn(0f, 1f)

                        val healthColor = Color4b.RED
                            .interpolateTo(Color4b.GREEN, healthPercentage.toDouble())
                        val healthHeight = rectHeight * healthPercentage

                        if (Border.enabled) {
                            drawVerticalLine(
                                x = -outlineThickness / 2 - borderThickness,
                                y1 = -outlineThickness / 2 - borderThickness,
                                y2 = rectHeight + outlineThickness / 2 + borderThickness,
                                outlineThickness + 2 * borderThickness, black
                            )
                        }

                        drawVerticalLine(
                            x = -outlineThickness / 2,
                            y1 = rectHeight - healthHeight - outlineThickness / 2,
                            y2 = rectHeight + outlineThickness / 2,
                            outlineThickness, healthColor
                        )
                    }
                }
            }
        }
    }

}

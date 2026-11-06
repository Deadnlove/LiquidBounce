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

import net.ccbluex.gratefuldead.event.events.WorldRenderEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.misc.FriendManager
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.render.GenericDistanceHSBColorMode
import net.ccbluex.gratefuldead.render.GenericEntityHealthColorMode
import net.ccbluex.gratefuldead.render.GenericRainbowColorMode
import net.ccbluex.gratefuldead.render.GenericStaticColorMode
import net.ccbluex.gratefuldead.render.drawLines
import net.ccbluex.gratefuldead.render.drawLinesWithWidth
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.render.engine.type.Vec3f
import net.ccbluex.gratefuldead.render.renderEnvironment
import net.ccbluex.gratefuldead.utils.combat.EntityTaggingManager
import net.ccbluex.gratefuldead.utils.entity.RenderedEntities
import net.ccbluex.gratefuldead.utils.entity.cameraDistanceSq
import net.ccbluex.gratefuldead.utils.entity.interpolateCurrentPosition
import net.ccbluex.gratefuldead.utils.math.sq
import net.ccbluex.gratefuldead.utils.math.toVec3f

/**
 * Tracers module
 *
 * Draws a line to every entity a certain radius.
 */

object ModuleTracers : ClientModule("Tracers", ModuleCategories.RENDER) {

    private val modes = choices("ColorMode", 0) {
        arrayOf(
            GenericDistanceHSBColorMode.entity(it),
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b(0, 160, 255, 255)),
            GenericRainbowColorMode(it)
        )
    }

    private val lineWidth by float("LineWidth", 1f, 1f..16f)

    private val maximumDistance by float("MaximumDistance", 128F, 1F..512F)

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        if (RenderedEntities.isEmpty()) {
            return@handler
        }

        event.renderEnvironment {
            val eyeVector = Vec3f.eyeVector(camera)

            val maxDistanceSq = maximumDistance.sq()
            for (entity in RenderedEntities) {
                val distanceSq = entity.position().cameraDistanceSq().toFloat()
                if (distanceSq > maxDistanceSq) {
                    continue
                }

                val color = if (FriendManager.isFriend(entity)) {
                    Color4b.BLUE
                } else {
                    EntityTaggingManager.getTag(entity).color ?: modes.activeMode.getColor(entity)
                }

                val pos = entity.interpolateCurrentPosition(event.partialTicks).subtract(camera.position()).toVec3f()
                val topPos = pos.add(0f, entity.bbHeight, 0f)

                if (lineWidth == 1.0f) {
                    drawLines(color.argb, eyeVector, pos, pos, topPos)
                } else {
                    drawLinesWithWidth(color.argb, lineWidth, eyeVector, pos, pos, topPos)
                }
            }
        }

    }
}

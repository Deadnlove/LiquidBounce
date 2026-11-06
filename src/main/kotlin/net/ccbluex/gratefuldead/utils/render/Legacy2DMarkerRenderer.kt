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
package net.ccbluex.gratefuldead.utils.render

import com.mojang.math.Axis
import net.ccbluex.gratefuldead.render.ClientRenderPipelines
import net.ccbluex.gratefuldead.render.WorldRenderEnvironment
import net.ccbluex.gratefuldead.render.drawCustomMesh
import net.ccbluex.gratefuldead.render.engine.type.Rect
import net.ccbluex.gratefuldead.render.esp.EspVisualStyle
import net.ccbluex.gratefuldead.render.esp.roundedPlaneLines
import net.minecraft.world.phys.AABB
import net.ccbluex.gratefuldead.render.withPositionRelativeToCamera
import net.minecraft.world.phys.Vec3

fun WorldRenderEnvironment.drawLegacy2DMarker(
    pos: Vec3,
    entityHeight: Double,
    scale: Float,
    foregroundArgb: Int,
    backgroundArgb: Int,
) {
    withPositionRelativeToCamera(pos) {
        poseStack.rotate(Axis.YP.rotationDegrees(-camera.yRot()))
        poseStack.scale(-scale, -scale, scale)

        drawLegacy2DRects(foregroundArgb, TOP_FOREGROUND_RECTS)
        drawLegacy2DRects(backgroundArgb, TOP_BACKGROUND_RECTS)

        poseStack.translate(0.0, 21.0 - entityHeight * 12.0, 0.0)

        drawLegacy2DRects(foregroundArgb, BOTTOM_FOREGROUND_RECTS)
        drawLegacy2DRects(backgroundArgb, BOTTOM_BACKGROUND_RECTS)
    }
}

private fun WorldRenderEnvironment.drawLegacy2DRects(color: Int, rects: Array<Rect>) {
    drawCustomMesh(ClientRenderPipelines.quads(noDepthTest = true)) { pose ->
        for (rect in rects) {
            val bounds = AABB(rect.x1.toDouble(), 0.0, rect.y1.toDouble(),
                rect.x2.toDouble(), 1.0, rect.y2.toDouble())
            val points = roundedPlaneLines(bounds, EspVisualStyle.GUI_RADIUS.toDouble(), 0.0)
            for (index in points.indices step 2) {
                val start = points[index]
                val end = points[index + 1]
                // A degenerate fourth vertex lets the quad pipeline draw a triangle fan.
                addVertex(pose, (rect.x1 + rect.x2) / 2, (rect.y1 + rect.y2) / 2, 0f).setColor(color)
                addVertex(pose, start.x.toFloat(), start.z.toFloat(), 0f).setColor(color)
                addVertex(pose, end.x.toFloat(), end.z.toFloat(), 0f).setColor(color)
                addVertex(pose, end.x.toFloat(), end.z.toFloat(), 0f).setColor(color)
            }
        }
    }
}

private val TOP_FOREGROUND_RECTS = arrayOf(
    Rect(-7f, 2f, -4f, 3f),
    Rect(4f, 2f, 7f, 3f),
    Rect(-7f, 0.5f, -6f, 3f),
    Rect(6f, 0.5f, 7f, 3f),
)

private val TOP_BACKGROUND_RECTS = arrayOf(
    Rect(-7f, 3f, -4f, 3.3f),
    Rect(4f, 3f, 7f, 3.3f),
    Rect(-7.3f, 0.5f, -7f, 3.3f),
    Rect(7f, 0.5f, 7.3f, 3.3f),
)

private val BOTTOM_FOREGROUND_RECTS = arrayOf(
    Rect(4f, -20f, 7f, -19f),
    Rect(-7f, -20f, -4f, -19f),
    Rect(6f, -20f, 7f, -17.5f),
    Rect(-7f, -20f, -6f, -17.5f),
)

private val BOTTOM_BACKGROUND_RECTS = arrayOf(
    Rect(7f, -20f, 7.3f, -17.5f),
    Rect(-7.3f, -20f, -7f, -17.5f),
    Rect(4f, -20.3f, 7.3f, -20f),
    Rect(-7.3f, -20.3f, -4f, -20f),
)

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

import net.ccbluex.gratefuldead.render.WorldRenderEnvironment
import net.ccbluex.gratefuldead.render.drawBox
import net.ccbluex.gratefuldead.render.ClientRenderPipelines
import net.ccbluex.gratefuldead.render.drawCustomMesh
import net.ccbluex.gratefuldead.render.drawBoxSide
import net.ccbluex.gratefuldead.render.drawLinesWithWidth
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.render.engine.type.Vec3f
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB

internal fun WorldRenderEnvironment.drawEspBox(
    box: AABB,
    faceColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
    faceVertices: Int = -1,
    outlineVertices: Int = -1,
) {
    drawBox(box, faceColor, null, faceVertices)
    if (outlineColor == null || outlineColor.isTransparent || outlineVertices == 0) return
    drawEspOutline(box, outlineColor, EspVisualStyle.lineWidth, outlineVertices)
}

internal fun WorldRenderEnvironment.drawEspPlane(box: AABB, side: Direction, face: Color4b, outline: Color4b?) {
    drawBoxSide(box, side, face, null)
    if (outline == null || outline.isTransparent) return
    val y = if (side == Direction.DOWN) box.minY else box.maxY
    val points = roundedPlaneLines(box, EspVisualStyle.CORNER_RADIUS, y)
    drawLinesWithWidth(outline.argb, EspVisualStyle.lineWidth, *points.map(::Vec3f).toTypedArray())
}

internal fun WorldRenderEnvironment.drawEspOutline(box: AABB, color: Color4b, width: Float, vertices: Int = -1) {
    if (color.isTransparent) return
    drawCustomMesh(ClientRenderPipelines.LinesWithWidth) { pose ->
        addRoundedOutline(pose, box, EspVisualStyle.CORNER_RADIUS, color, width, vertices)
    }
}

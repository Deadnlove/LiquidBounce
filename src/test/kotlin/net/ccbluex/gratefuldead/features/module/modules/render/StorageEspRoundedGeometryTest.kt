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

import net.ccbluex.gratefuldead.render.BoxVertexIterator
import net.ccbluex.gratefuldead.render.esp.roundedPlaneLines
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import net.ccbluex.gratefuldead.render.esp.roundedBoxLines
import net.ccbluex.gratefuldead.render.esp.roundedElbow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageEspRoundedGeometryTest {
    @Test
    fun `rounded strokes stay inside the box and contain no zero-length segments`() {
        val box = AABB(-2.0, 60.0, 3.0, 1.0, 62.0, 4.0)
        val points = roundedBoxLines(box, 0.12)
        assertEquals(312, points.size)
        for (point in points) {
            assertTrue(point.x in box.minX..box.maxX)
            assertTrue(point.y in box.minY..box.maxY)
            assertTrue(point.z in box.minZ..box.maxZ)
        }
        for (index in points.indices step 2) assertTrue(points[index].distanceToSqr(points[index + 1]) > 1E-12)
        val corners = listOf(Vec3(box.minX, box.minY, box.minZ), Vec3(box.maxX, box.maxY, box.maxZ))
        assertTrue(points.none { it in corners })
    }

    @Test
    fun `zero radius preserves twelve edges and thin boxes clamp the radius`() {
        val box = AABB(0.0, 0.0, 0.0, 1.0, 0.01, 1.0)
        assertEquals(24, roundedBoxLines(box, 0.0).size)
        val points = roundedBoxLines(box, 0.25)
        assertTrue(points.all { it.y >= box.minY && it.y <= box.maxY })
        assertTrue(roundedBoxLines(AABB(0.0, 0.0, 0.0, 0.0, 1.0, 1.0), 0.12).isEmpty())
    }

    @Test
    fun `elbow curve keeps tracer endpoints and removes the right angle junction`() {
        val start = Vec3(0.0, 0.0, 0.0)
        val end = Vec3(10.0, 4.0, 10.0)
        val points = roundedElbow(start, end)
        assertEquals(start, points.first())
        assertEquals(end, points.last())
        assertTrue(Vec3(end.x, start.y, end.z) !in points)
        for (index in points.indices step 2) assertTrue(points[index].distanceToSqr(points[index + 1]) > 1E-12)
        assertEquals(listOf(start, Vec3(0.0, 10.0, 0.0)), roundedElbow(start, Vec3(0.0, 10.0, 0.0)))
    }
    @Test
    fun `culled face outlines and planar loops keep their plane and paired endpoints`() {
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        val face = roundedBoxLines(box, 0.12, vertices = BoxVertexIterator.OUTLINE.sideMask(Direction.DOWN))
        assertEquals(56, face.size)
        assertTrue(face.all { it.y == 0.0 })
        val plane = roundedPlaneLines(box, 0.12, 1.0)
        assertEquals(0, plane.size % 2)
        assertTrue(plane.all { it.y == 1.0 && it.x in 0.0..1.0 && it.z in 0.0..1.0 })
        assertEquals(plane.first(), plane.last())
    }

}

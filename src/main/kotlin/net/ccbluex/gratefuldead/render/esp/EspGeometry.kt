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

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ccbluex.gratefuldead.render.addVertex
import net.ccbluex.gratefuldead.render.forEachOutlineVertex
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.render.engine.type.Vec3f
import net.ccbluex.gratefuldead.render.setNormal
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

/** Twelve trimmed edges joined tangentially by quarter circles in each corner plane. */
internal fun roundedBoxLines(box: AABB, radius: Double, steps: Int = 6, vertices: Int = -1): List<Vec3> {
    val key = EspOutlineKey(box.xsize, box.ysize, box.zsize, radius, steps, vertices)
    val points = synchronized(espOutlineCache) {
        espOutlineCache.getOrPut(key) {
            buildRoundedBoxLines(AABB(0.0, 0.0, 0.0, key.x, key.y, key.z), radius, steps, vertices)
        }
    }
    return if (box.minX == 0.0 && box.minY == 0.0 && box.minZ == 0.0) {
        points
    } else {
        points.map { it.add(box.minX, box.minY, box.minZ) }
    }
}

private data class EspOutlineKey(val x: Double, val y: Double, val z: Double,
    val radius: Double, val steps: Int, val vertices: Int)

private val espOutlineCache = object : LinkedHashMap<EspOutlineKey, List<Vec3>>(128, .75F, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<EspOutlineKey, List<Vec3>>?) = size > 512
}

private fun buildRoundedBoxLines(box: AABB, radius: Double, steps: Int, vertices: Int): List<Vec3> {
    require(radius.isFinite() && radius >= 0.0 && steps > 0)
    val size = minOf(box.xsize, box.ysize, box.zsize)
    if (size <= 1E-6) return emptyList()
    val r = radius.coerceAtMost(size * 0.49).takeIf { it >= 1E-4 } ?: 0.0
    val corners = storageBoxCorners(box)
    val edges = visibleStorageEdges(box, vertices)
    val axes = arrayOf(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Vec3(0.0, 0.0, 1.0))
    return buildList {
        appendStorageEdges(corners, axes, r, edges)
        if (r > 0.0) {
            for (bits in 0..7) {
                val inward = axes.mapIndexed { axis, vector ->
                    vector.scale(if (bits and (1 shl axis) == 0) r else -r)
                }
                appendStorageCorner(corners[bits], inward, steps, (0..2).filter { bits to it in edges })
            }
        }
    }
}

private fun visibleStorageEdges(box: AABB, vertices: Int): Set<Pair<Int, Int>> {
    val edges = mutableSetOf<Pair<Int, Int>>()
    box.forEachOutlineVertex { index, x, y, z ->
        if (index and 1 != 0 || vertices ushr index and 3 == 0) return@forEachOutlineVertex
        val bits = (if (x == box.maxX) 1 else 0) or (if (y == box.maxY) 2 else 0) or
            (if (z == box.maxZ) 4 else 0)
        val axis = when (index / 2) {
            0, 2, 8, 10 -> 0
            1, 3, 9, 11 -> 2
            else -> 1
        }
        val base = bits and (1 shl axis).inv()
        edges.add(base to axis)
        edges.add((base or (1 shl axis)) to axis)
    }
    return edges
}

private fun storageBoxCorners(box: AABB) = (0..7).map { bits -> Vec3(
    if (bits and 1 == 0) box.minX else box.maxX,
    if (bits and 2 == 0) box.minY else box.maxY,
    if (bits and 4 == 0) box.minZ else box.maxZ,
) }

private fun MutableList<Vec3>.appendStorageEdges(
    corners: List<Vec3>,
    axes: Array<Vec3>,
    radius: Double,
    edges: Set<Pair<Int, Int>>,
) {
    for (bits in 0..7) {
        for (axis in 0..2) {
            if (bits and (1 shl axis) == 0 && bits to axis in edges) {
                add(corners[bits].add(axes[axis].scale(radius)))
                add(corners[bits or (1 shl axis)].subtract(axes[axis].scale(radius)))
            }
        }
    }
}

private fun MutableList<Vec3>.appendStorageCorner(
    corner: Vec3, inward: List<Vec3>, steps: Int, visibleAxes: List<Int>,
) {
    for ((a, b) in listOf(0 to 1, 0 to 2, 1 to 2)) {
        if (a !in visibleAxes || b !in visibleAxes) continue
        val first = inward[a]
        val second = inward[b]
        val center = corner.add(first).add(second)
        var previous = center.subtract(first)
        for (step in 1..steps) {
            val angle = step * Math.PI / (2.0 * steps)
            val next = center.subtract(first.scale(cos(angle))).subtract(second.scale(sin(angle)))
            add(previous)
            add(next)
            previous = next
        }
    }
}

internal fun roundedElbow(start: Vec3, end: Vec3): List<Vec3> {
    val bend = Vec3(end.x, start.y, end.z)
    val incoming = bend.subtract(start)
    val outgoing = end.subtract(bend)
    val radius = minOf(0.5, incoming.length() * 0.25, outgoing.length() * 0.25)
    if (radius < 1E-5) return listOf(start, end)
    val before = bend.subtract(incoming.normalize().scale(radius))
    val after = bend.add(outgoing.normalize().scale(radius))
    return buildList {
        add(start)
        add(before)
        var previous = before
        for (step in 1..8) {
            val t = step / 8.0
            val next = before.scale((1.0 - t) * (1.0 - t)).add(bend.scale(2.0 * (1.0 - t) * t))
                .add(after.scale(t * t))
            add(previous)
            add(next)
            previous = next
        }
        add(after)
        add(end)
    }
}

internal fun VertexConsumer.addRoundedOutline(
    pose: PoseStack.Pose,
    box: AABB,
    radius: Double,
    color: Color4b,
    width: Float,
    vertices: Int = -1,
) {
    val points = roundedBoxLines(box, radius, vertices = vertices)
    for (index in points.indices step 2) {
        val start = Vec3f(points[index])
        val end = Vec3f(points[index + 1])
        if ((end - start).lengthSqr() < 1E-12F) continue
        val normal = (end - start).normalized()
        addVertex(pose, start).setColor(color.argb).setNormal(pose, normal).setLineWidth(width)
        addVertex(pose, end).setColor(color.argb).setNormal(pose, -normal).setLineWidth(width)
    }
}

/** A planar rounded loop, emitted as independent line segments. */
internal fun roundedPlaneLines(box: AABB, radius: Double, y: Double): List<Vec3> {
    val r = radius.coerceIn(0.0, minOf(box.xsize, box.zsize) * 0.49)
    val centers = listOf(
        Vec3(box.maxX - r, y, box.maxZ - r), Vec3(box.minX + r, y, box.maxZ - r),
        Vec3(box.minX + r, y, box.minZ + r), Vec3(box.maxX - r, y, box.minZ + r),
    )
    val loop = centers.flatMapIndexed { index, center ->
        (0..6).map { step ->
            val angle = (index * 90.0 + step * 15.0) * Math.PI / 180.0
            center.add(cos(angle) * r, 0.0, sin(angle) * r)
        }
    }
    return loop.indices.flatMap { index -> listOf(loop[index], loop[(index + 1) % loop.size]) }
}

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

import net.ccbluex.gratefuldead.config.types.group.ToggleableValueGroup
import net.ccbluex.gratefuldead.event.events.GameTickEvent
import net.ccbluex.gratefuldead.event.events.OverlayRenderEvent
import net.ccbluex.gratefuldead.event.events.WorldChangeEvent
import net.ccbluex.gratefuldead.event.events.WorldRenderEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.render.WorldRenderEnvironment
import net.ccbluex.gratefuldead.render.gui.indicator.IndicatorRenderer
import net.ccbluex.gratefuldead.render.drawLinesWithWidth
import net.ccbluex.gratefuldead.render.esp.drawEspOutline
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.render.engine.type.Vec3f
import net.ccbluex.gratefuldead.render.renderEnvironment
import net.ccbluex.gratefuldead.utils.entity.cameraDistanceSq
import net.ccbluex.gratefuldead.utils.math.toVec3f
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.LidBlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.sin

/** Temporal accents use only live client state. Unloaded entities are never simulated. */
internal class StorageEspMotion(
    private val blocks: () -> Sequence<BlockPos>,
    private val topology: () -> StorageTopologyGraph,
    private val fadedColor: (Color4b, Double) -> Color4b,
    private val lineWidth: () -> Float,
) : ToggleableValueGroup(ModuleStorageESP, "Motion", true) {
    private val statePulse by boolean("StatePulse", true)
    private val trails by boolean("Trails", true)
    private val movementArcs by boolean("MovementArcs", true)
    private val selection by boolean("Selection", true)
    private val focusLabel by boolean("FocusLabel", true)
    private val focusColor by color("FocusColor", Color4b(255, 225, 125))
    private val clusterHull by boolean("ClusterHull", false)
    private val fadeSeconds by float("FadeSeconds", 0.25F, 0.1F..1F)
    private val trailSeconds by float("TrailSeconds", 0.7F, 0.2F..2F)
    private val minimumSpeed by float("MinimumSpeed", 0.12F, 0.02F..1F)
    private val pulseStrength by float("PulseStrength", 0.15F, 0F..0.5F)

    private data class TrailPoint(val pos: Vec3, val time: Double)
    private class Track(val fade: StorageEspTransition, var color: Color4b, position: Vec3, var box: AABB) {
        var previous = position
        var pos = position
        var seen = false
        val history = ArrayDeque<TrailPoint>()
    }

    private val tracks = LinkedHashMap<UUID, Track>()
    private var selectionFade = StorageEspTransition(0.25)
    private var hullFade = StorageEspTransition(0.25)
    private var selectedBox: AABB? = null
    private var selectedName = ""
    private var hullTarget: AABB? = null
    private var hullBox: AABB? = null
    private var lastFrame = 0.0

    val needsTopology: Boolean get() = running && clusterHull

    val fadeDuration: Double
        get() = fadeSeconds.toDouble()

    private val nowSeconds: Double
        get() = System.nanoTime() / 1E9

    fun opacity(entity: Entity): Double = if (running) tracks[entity.uuid]?.fade?.sample(nowSeconds) ?: 1.0 else 1.0

    override fun onDisabled() = clear()

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> { clear() }

    private fun clear() {
        tracks.clear()
        selectedBox = null
        hullTarget = null
        hullBox = null
        selectionFade = StorageEspTransition(0.25)
        hullFade = StorageEspTransition(0.25)
        lastFrame = 0.0
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val now = nowSeconds
        tracks.values.forEach { it.seen = false }
        val candidates = world.entitiesForRendering().asSequence().filter {
            with(ModuleStorageESP) { it.categorize() }?.let { type ->
                !type.color.isTransparent && type.shouldRender(it)
            } == true
        }
        val storages = nearestStorageCandidates(candidates, StorageEspCapacity.animatedEntities) {
            it.position().cameraDistanceSq()
        }
        for (entity in storages) {
            val type = with(ModuleStorageESP) { entity.categorize() } ?: continue
            if (entity.uuid !in tracks && tracks.size >= StorageEspCapacity.animatedEntities) continue
            val track = tracks.getOrPut(entity.uuid) {
                Track(StorageEspTransition(fadeSeconds.toDouble()), type.color, entity.position(), entity.boundingBox)
            }
            track.seen = true
            track.color = type.color
            track.previous = track.pos
            track.pos = entity.position()
            track.box = entity.boundingBox
            track.fade.show(true, now)
            if (trails && track.pos.distanceToSqr(track.previous) >= minimumSpeed * minimumSpeed) {
                track.history.addLast(TrailPoint(track.previous, now))
                while (track.history.size > StorageEspCapacity.trailPoints) track.history.removeFirst()
            }
        }
        tracks.values.removeIf { track ->
            if (!track.seen) track.fade.show(false, now)
            while (track.history.firstOrNull()?.let { now - it.time > trailSeconds } == true) {
                track.history.removeFirst()
            }
            !track.seen && track.fade.sample(now) <= 0.001
        }
        hullTarget = if (clusterHull) findCluster() else null
        hullFade.show(hullTarget != null, now)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val now = nowSeconds
        val smoothing = storageSmoothing(if (lastFrame == 0.0) 0.0 else now - lastFrame)
        lastFrame = now
        event.renderEnvironment {
            val width = lineWidth()
            drawEntities(now, event.partialTicks, width)
            if (statePulse) drawStatePulse(now, event.partialTicks, width)
            val target = if (selection) selectionTarget() else null
            selectionFade.show(target != null, now)
            if (target != null) selectedBox = selectedBox?.let { smoothBox(it, target, smoothing) } ?: target
            selectedBox?.let {
                drawHull(it.inflate(0.035), focusColor, selectionFade.sample(now), width * 1.25F)
            }
            hullTarget?.let { targetHull ->
                hullBox = hullBox?.let { smoothBox(it, targetHull, smoothing) } ?: targetHull
            }
            hullBox?.let {
                drawHull(it.inflate(0.1), Color4b(110, 185, 255), hullFade.sample(now) * 0.45, width)
            }
        }
    }

    @Suppress("unused")
    private val labelHandler = handler<OverlayRenderEvent> {
        val box = selectedBox
        val opacity = selectionFade.sample(nowSeconds)
        if (focusLabel && box != null && opacity > 0.01) {
            val pos = Vec3(box.center.x, box.maxY + 0.25, box.center.z)
            val distance = kotlin.math.sqrt(box.center.cameraDistanceSq()).toInt()
            val color = fadedColor(focusColor, box.center.cameraDistanceSq()).fade(opacity.toFloat())
            if (!color.isTransparent) {
                IndicatorRenderer.submit(StorageEspFocusLabel(pos, color, "$selectedName · ${distance}m"))
            }
        }
    }

    private fun WorldRenderEnvironment.drawEntities(now: Double, partialTicks: Float, width: Float) {
        for (track in tracks.values) {
            val alpha = track.fade.sample(now)
            val pos = track.previous.lerp(track.pos, partialTicks.toDouble())
            val velocity = track.pos.subtract(track.previous)
            val fast = velocity.lengthSqr() >= minimumSpeed * minimumSpeed
            if (!track.seen || (statePulse && fast)) {
                val pulse = 1.0 - pulseStrength + pulseStrength * sin(now * 4.0) * 0.5
                drawHull(track.box.move(pos.subtract(track.pos)), track.color, alpha * pulse, width)
            }
            if (trails) drawTrail(track, pos, now, alpha, width)
            if (movementArcs && fast && track.seen) drawMovementArc(pos, velocity, track.color, alpha, width)
        }
    }

    private fun WorldRenderEnvironment.drawTrail(track: Track, pos: Vec3, now: Double, alpha: Double, width: Float) {
        val points = track.history.toList()
        for (index in points.indices) {
            val point = points[index]
            val end = points.getOrNull(index + 1)?.pos ?: pos
            // A teleport must not draw a world-spanning streak.
            if (point.pos.distanceToSqr(end) !in 1E-6..64.0) continue
            val opacity = (1.0 - (now - point.time) / trailSeconds).coerceIn(0.0, 1.0)
            val color = fadedColor(track.color, point.pos.cameraDistanceSq())
            drawLinesWithWidth(color.with(a = (color.a * alpha * opacity * 0.6).toInt()).argb, width,
                point.pos.subtract(camera.position()).toVec3f(), end.subtract(camera.position()).toVec3f())
        }
    }

    private fun WorldRenderEnvironment.drawMovementArc(
        pos: Vec3,
        velocity: Vec3,
        color: Color4b,
        alpha: Double,
        width: Float,
    ) {
        if (velocity.lengthSqr() > 64.0) return
        var previous = pos
        for (index in 1..12) {
            val t = index / 12.0
            val next = pos.add(velocity.scale(t * 6.0)).add(0.0, sin(t * Math.PI) * 0.35, 0.0)
            val faded = fadedColor(color, next.cameraDistanceSq())
            drawLinesWithWidth(faded.with(a = (faded.a * alpha * (1.0 - t) * 0.6).toInt()).argb, width,
                previous.subtract(camera.position()).toVec3f(), next.subtract(camera.position()).toVec3f())
            previous = next
        }
    }

    private fun WorldRenderEnvironment.drawStatePulse(now: Double, partialTicks: Float, width: Float) {
        for (pos in blocks().take(StorageEspCapacity.pulseBlocks)) {
            val state = world.getBlockState(pos)
            val blockEntity = world.getBlockEntity(pos)
            val opening = (blockEntity as? LidBlockEntity)?.getOpenNess(partialTicks)?.toDouble() ?: 0.0
            val active = state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)
            if (!active && opening <= 0.001) continue
            val type = with(ModuleStorageESP) { blockEntity.categorize() } ?: continue
            val shape = state.getShape(world, pos)
            if (shape.isEmpty) continue
            val pulse = pulseStrength * maxOf(opening, if (active) 0.5 + 0.5 * sin(now * 4.0) else 0.0)
            drawHull(shape.bounds().move(pos), type.color, pulse, width)
        }
    }

    private fun selectionTarget(): AABB? {
        val hit = mc.hitResult
        val entity = (hit as? EntityHitResult)?.entity
        if (entity != null) {
            val type = with(ModuleStorageESP) { entity.categorize() } ?: return null
            if (!type.shouldRender(entity) || type.color.isTransparent) return null
            selectedName = type.name
            return entity.boundingBox
        }
        val pos = (hit as? BlockHitResult)?.blockPos ?: return null
        val type = with(ModuleStorageESP) { world.getBlockEntity(pos).categorize() } ?: return null
        if (!type.shouldRender(pos) || type.color.isTransparent) return null
        val shape = world.getBlockState(pos).getShape(world, pos)
        if (shape.isEmpty) return null
        selectedName = storageChestLabel(world.getBlockState(pos), type.name)
        return storageChestBounds(pos, world.getBlockState(pos), shape.bounds().move(pos), world::getBlockState)
    }

    private fun findCluster(): AABB? {
        val range = StorageEspCapacity.clusterRange.toDouble()
        val positions = blocks().filter { it.cameraDistanceSq() < range * range }
            .take(StorageEspCapacity.clusterCandidates).toMutableSet()
        val focused = (mc.hitResult as? BlockHitResult)?.blockPos?.takeIf { it in positions }
        val first = focused ?: positions.minByOrNull { it.cameraDistanceSq() } ?: return null
        val component = topology().component(first, positions, StorageEspCapacity.clusterBlocks)
        if (component.size < 2) return null
        return component.fold(AABB(first)) { box, pos -> box.minmax(AABB(pos)) }
    }

    private fun WorldRenderEnvironment.drawHull(box: AABB, color: Color4b, alpha: Double, width: Float) {
        val faded = fadedColor(color, box.center.cameraDistanceSq())
        val opacity = (faded.a * alpha).toInt().coerceIn(0, 255)
        if (opacity == 0) return
        drawEspOutline(box.move(camera.position().reverse()), faded.with(a = opacity), width)
    }

    private fun smoothBox(from: AABB, to: AABB, factor: Double) = AABB(
        from.minX + (to.minX - from.minX) * factor, from.minY + (to.minY - from.minY) * factor,
        from.minZ + (to.minZ - from.minZ) * factor, from.maxX + (to.maxX - from.maxX) * factor,
        from.maxY + (to.maxY - from.maxY) * factor, from.maxZ + (to.maxZ - from.maxZ) * factor,
    )
}

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
import net.ccbluex.gratefuldead.render.esp.drawEspOutline
import net.ccbluex.gratefuldead.render.esp.roundedElbow
import net.ccbluex.gratefuldead.render.esp.addRoundedOutline
import kotlinx.atomicfu.atomic
import net.ccbluex.gratefuldead.config.types.group.Mode
import net.ccbluex.gratefuldead.config.types.list.Tagged
import net.ccbluex.gratefuldead.config.types.group.ModeValueGroup
import net.ccbluex.gratefuldead.config.types.group.ToggleableValueGroup
import net.ccbluex.gratefuldead.event.events.DrawOutlinesEvent
import net.ccbluex.gratefuldead.event.events.GameTickEvent
import net.ccbluex.gratefuldead.event.events.WorldRenderEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.gratefuldead.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.gratefuldead.render.CachedMeshStorage
import net.ccbluex.gratefuldead.render.ClientRenderPipelines
import net.ccbluex.gratefuldead.render.addShapeFaces
import net.ccbluex.gratefuldead.render.buildMesh
import net.ccbluex.gratefuldead.render.drawBox
import net.ccbluex.gratefuldead.render.drawGenericBlockESP
import net.ccbluex.gratefuldead.render.drawLinesWithWidth
import net.ccbluex.gratefuldead.render.WorldRenderEnvironment
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.render.engine.type.Vec3f
import net.ccbluex.gratefuldead.render.getDynamicTransformsUniform
import net.ccbluex.gratefuldead.render.renderEnvironment
import net.ccbluex.gratefuldead.render.translate
import net.ccbluex.gratefuldead.render.utils.DistanceFadeUniformValueGroup
import net.ccbluex.gratefuldead.render.withPositionRelativeToCamera
import net.ccbluex.gratefuldead.render.withPush
import net.ccbluex.gratefuldead.utils.block.ChunkScanner
import net.ccbluex.gratefuldead.utils.entity.cameraDistanceSq
import net.ccbluex.gratefuldead.utils.entity.interpolateCurrentPosition
import net.ccbluex.gratefuldead.utils.math.PositionedVoxelShape
import net.ccbluex.gratefuldead.utils.math.center
import net.ccbluex.gratefuldead.utils.math.mergeAdjacentVoxelShapes
import net.ccbluex.gratefuldead.utils.math.sq
import net.ccbluex.gratefuldead.utils.math.toVec3f
import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse
import net.minecraft.world.entity.vehicle.boat.ChestBoat
import net.minecraft.world.entity.vehicle.boat.ChestRaft
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity
import net.minecraft.world.level.block.entity.CrafterBlockEntity
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity
import net.minecraft.world.level.block.entity.DispenserBlockEntity
import net.minecraft.world.level.block.entity.EnderChestBlockEntity
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.block.entity.ShelfBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.VoxelShape
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * StorageESP module
 *
 * Allows you to see chests, dispensers, etc. through walls.
 */

object ModuleStorageESP : ClientModule("StorageESP", ModuleCategories.RENDER, aliases = listOf("ChestESP")) {

    private val modes = choices("Mode", BoxMode, arrayOf(BoxMode, GlowMode))

    sealed class ChestType(name: String, defaultColor: Color4b) : ToggleableValueGroup(this, name, enabled = true) {
        val color by color("Color", defaultColor).onChanged { markDirtyForModes() }

        override fun onEnabled() = markDirtyForModes()
        override fun onDisabled() = markDirtyForModes()
        val tracers by boolean("Tracers", false)

        @JvmOverloads
        fun shouldRender(pos: BlockPos, ignoreDistance: Boolean = false): Boolean =
            this.running
                && pos !in FeatureChestAura.interactedBlocksSet
                && (ignoreDistance || pos.cameraDistanceSq() < distanceFade.farEnd.sq())

        fun renderColor(pos: BlockPos): Color4b = fadedColor(color, pos.cameraDistanceSq()).fade(
            if (motion.running) blockAnimations.opacity(pos.asLong()).toFloat() else 1F
        )

        fun renderColor(entity: Entity): Color4b {
            val faded = fadedColor(color, entity.position().cameraDistanceSq())
            return faded.with(a = (faded.a * motion.opacity(entity)).toInt())
        }

        @JvmOverloads
        fun shouldRender(entity: Entity, ignoreDistance: Boolean = false): Boolean =
            this.running
                && (ignoreDistance || entity.position().cameraDistanceSq() < distanceFade.farEnd.sq())

        object Chest : ChestType("Chest", Color4b(0, 100, 255))
        object TrappedChest : ChestType("TrappedChest", Color4b(255, 100, 105))
        object Barrel : ChestType("Barrel", Color4b(0xf6, 0x82, 0x1f))
        object EnderChest : ChestType("EnderChest", Color4b(Color.MAGENTA))
        object Furnace : ChestType("Furnace", Color4b(79, 79, 79))
        object BrewingStand : ChestType("BrewingStand", Color4b(139, 69, 19))
        object Dispenser : ChestType("Dispenser", Color4b(Color.LIGHT_GRAY))
        object Hopper : ChestType("Hopper", Color4b(Color.GRAY))
        object ShulkerBox : ChestType("ShulkerBox", Color4b(Color(0x6e, 0x4d, 0x6e).brighter()))
        object Pot : ChestType("Pot", Color4b(209, 134, 0))
        object Bookshelf : ChestType("Bookshelf", Color4b(139, 90, 43))
        object Shelf : ChestType("Shelf", Color4b(160, 82, 45))
    }

    private val allTypes = arrayOf(
        ChestType.Chest,
        ChestType.TrappedChest,
        ChestType.Barrel,
        ChestType.EnderChest,
        ChestType.Furnace,
        ChestType.BrewingStand,
        ChestType.Dispenser,
        ChestType.Hopper,
        ChestType.ShulkerBox,
        ChestType.Pot,
        ChestType.Bookshelf,
        ChestType.Shelf,
    )

    init {
        allTypes.forEach { tree(it) }
    }

    private val requiresChestStealer by boolean("RequiresChestStealer", false)
    private val mergeAdjacent by boolean("MergeAdjacent", false).onChanged {
        markDirtyForModes()
    }

    private val joinDoubleChests by boolean("JoinDoubleChests", true).onChanged { markDirtyForModes() }

    // 256 chunks is a configurable display limit; the server still controls which chunks arrive.
    private val distanceFade = tree(
        DistanceFadeUniformValueGroup(maxDistance = 4096F, defaultFarStart = 1536F, defaultFarEnd = 2048F)
    )
    private val lineWidth by float("LineWidth", 1.5F, 1F..6F).onChanged { markDirtyForModes() }
    private val scaleWithResolution by boolean("ScaleWithResolution", true).onChanged { markDirtyForModes() }
    private val tracerStyle by enumChoice("TracerStyle", TracerStyle.DIRECT)
    init {
        tree(Connections)
        tree(StorageEspCapacity)
    }
    private val rememberUnloaded by boolean("RememberUnloaded", false).onChanged {
        StorageScanner.forgetUnloaded()
    }

    private enum class TracerStyle(override val tag: String) : Tagged {
        DIRECT("Direct"), ELBOW("Elbow")
    }

    private val motion = tree(
        StorageEspMotion(
            blocks = {
                StorageScanner.iterate().filter { (pos, type) ->
                    !type.color.isTransparent && type.shouldRender(pos)
                }.map { it.key }
            },
            topology = { StorageTopology.graph },
            fadedColor = ::fadedColor,
            lineWidth = { lineWidth * if (scaleWithResolution) (mc.window.width / 1920F).coerceAtLeast(1F) else 1F },
        )
    )

    private val blockAnimations = StorageEspBlockAnimation<PositionedVoxelShape<ChestType>>(
        capacity = 32768,
        duration = { motion.fadeDuration },
    )

    private var lastRenderWidth = 0
    private var lastInteractedBlocks = emptySet<BlockPos>()

    @Suppress("unused")
    private val refreshHandler = handler<GameTickEvent> {
        if (lastRenderWidth != mc.window.width) {
            lastRenderWidth = mc.window.width
            markDirtyForModes()
        }
        val interacted = FeatureChestAura.interactedBlocksSet
        if (lastInteractedBlocks != interacted) {
            lastInteractedBlocks = interacted.toSet()
            markDirtyForModes()
        }
        StorageTopology.refresh()
        val expired = StorageScanner.expireMemories()
        val animating = blockAnimations.advance()
        if (expired || animating) markDirtyForModes(topology = false)
    }

    private fun fadedColor(color: Color4b, distanceSq: Double): Color4b =
        color.with(a = (color.a * distanceFade.alphaAtDistance(sqrt(distanceSq).toFloat())).toInt())

    override fun onEnabled() {
        ChunkScanner.subscribe(StorageScanner)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(StorageScanner)
        lastInteractedBlocks = emptySet()
    }

    private object BoxMode : Mode("Box") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val dirtyFlag = atomic(true)

        private val blockFacesRenderState = CachedMeshStorage("${ModuleStorageESP.name} $name BlockFaces")
        private val blockOutlinesRenderState = CachedMeshStorage("${ModuleStorageESP.name} $name BlockOutlines")

        fun markDirty() {
            if (this.running) {
                dirtyFlag.value = true
            }
        }

        override fun enable() {
            dirtyFlag.value = true
            super.enable()
        }

        private val outline by boolean("Outline", true).onChanged { markDirty() }
        private val fillOpacity by int("FillOpacity", 50, 0..255).onChanged { markDirty() }
        private val outlineOpacity by int("OutlineOpacity", 100, 0..255).onChanged { markDirty() }

        private val entityBoxes = mutableListOf<EntityBox>()

        override fun disable() {
            blockFacesRenderState.clearStates()
            blockFacesRenderState.clearBuffers()
            blockOutlinesRenderState.clearStates()
            blockOutlinesRenderState.clearBuffers()
            entityBoxes.clear()
            super.disable()
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (outline) {
                mc.gameRenderer.mainRenderTarget().drawGenericBlockESP(
                    renderState = blockOutlinesRenderState,
                    pipeline = ClientRenderPipelines.EspLines,
                    distanceFade = distanceFade,
                ) {
                    getDynamicTransformsUniform(
                        modelView = event.poseStack.last().pose(),
                    )
                }
            }

            mc.gameRenderer.mainRenderTarget().drawGenericBlockESP(
                renderState = blockFacesRenderState,
                pipeline = ClientRenderPipelines.relativeQuads(useColor = true),
                distanceFade = distanceFade,
            ) {
                getDynamicTransformsUniform(
                    modelView = event.poseStack.last().pose(),
                )
            }

            if (entityBoxes.isEmpty()) return@handler

            val matrixStack = event.poseStack

            event.renderEnvironment {
                for ((entity, box, color) in entityBoxes) {
                    val distanceColor = fadedColor(color, entity.position().cameraDistanceSq())
                    val faded = distanceColor.with(a = (distanceColor.a * motion.opacity(entity)).toInt())
                    val baseColor = faded.with(a = faded.a * fillOpacity / 255)
                    val outlineColor = if (outline) faded.with(a = faded.a * outlineOpacity / 255) else null

                    val pos = entity.interpolateCurrentPosition(event.partialTicks)
                    withPositionRelativeToCamera(pos) {
                        drawBox(box, baseColor, null)
                        if (outlineColor != null && !outlineColor.isTransparent) {
                            val width = lineWidth * if (scaleWithResolution) {
                                (mc.window.width / 1920F).coerceAtLeast(1F)
                            } else {
                                1F
                            }
                            drawEspOutline(box, outlineColor, width)
                        }
                    }
                }
            }
        }

        @JvmRecord
        private data class EntityBox(val entity: Entity, val box: AABB, val color: Color4b)

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            val level = mc.level ?: return@handler

            entityBoxes.clear()

            for (entity in level.entitiesForRendering()) {
                val type = entity.categorize()?.takeIf {
                    !it.color.isTransparent && it.shouldRender(entity)
                } ?: continue

                val dimensions = entity.getDimensions(entity.pose)
                val d = dimensions.width.toDouble() / 2.0
                val box = AABB(-d, 0.0, -d, d, dimensions.height.toDouble(), d).inflate(0.05)

                entityBoxes.add(EntityBox(entity, box, type.color))
            }


            if (StorageScanner.isEmpty) {
                blockFacesRenderState.clearStates()
                blockOutlinesRenderState.clearStates()
                return@handler
            }

            if (!dirtyFlag.compareAndSet(expect = true, update = false)) {
                return@handler
            }

            val mergedShapes = collectTrackedBlockShapes()

            blockFacesRenderState.buildMesh(
                pipeline = ClientRenderPipelines.relativeQuads(useColor = true),
                origin = player.blockPosition(),
            ) { pose, origin ->
                for (mergedShape in mergedShapes) {
                    pose.withPush {
                        translate(mergedShape.blockPos, origin)
                        val color = mergedShape.key.color
                        addShapeFaces(last().pose(), mergedShape.shape, color.with(a = color.a * fillOpacity / 255))
                    }
                }
            }

            if (outline) {
                blockOutlinesRenderState.buildMesh(
                    pipeline = ClientRenderPipelines.EspLines,
                    origin = player.blockPosition(),
                ) { pose, origin ->
                    for (mergedShape in mergedShapes) {
                        pose.withPush {
                            translate(mergedShape.blockPos, origin)
                            val color = mergedShape.key.color
                            val width = lineWidth * if (scaleWithResolution) {
                                (mc.window.width / 1920F).coerceAtLeast(1F)
                            } else {
                                1F
                            }
                            if (!mergedShape.shape.isEmpty) {
                                addRoundedOutline(
                                    last(), mergedShape.shape.bounds(), EspVisualStyle.CORNER_RADIUS,
                                    color.with(a = color.a * outlineOpacity / 255), width,
                                )
                            }
                        }
                    }
                }
            }
        }

    }

    object GlowMode : Mode("Glow") {
        // Geometry is independent of vanilla block entity renderer distance/frustum culling.
        val fullRange by boolean("FullRange", true).onChanged { markDirty() }
        private val dirtyFlag = atomic(true)

        private val renderState = CachedMeshStorage("${ModuleStorageESP.name} $name")

        internal fun markDirty() {
            if (this.running) {
                dirtyFlag.value = true
            }
        }

        override fun enable() {
            dirtyFlag.value = true
            super.enable()
        }

        override fun disable() {
            renderState.clearStates()
            renderState.clearBuffers()
            super.disable()
        }

        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused")
        private val glowRenderHandler = handler<DrawOutlinesEvent> { event ->
            val dirty = event.renderTarget.drawGenericBlockESP(
                renderState = renderState,
                pipeline = ClientRenderPipelines.outlineQuads(useColor = true),
                distanceFade = distanceFade,
            )

            if (dirty) {
                event.markDirty()
            }
        }

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            if (StorageScanner.isEmpty) {
                renderState.clearStates()
                return@handler
            }

            if (!dirtyFlag.compareAndSet(expect = true, update = false)) {
                return@handler
            }

            renderState.buildMesh(
                pipeline = ClientRenderPipelines.outlineQuads(useColor = true),
                origin = player.blockPosition(),
            ) { pose, origin ->
                // Model mode preserves animated chest lids; FullRange uses cached block shapes instead.
                for (mergedShape in collectTrackedBlockShapes { !fullRange && it.renderShape != RenderShape.MODEL }) {
                    pose.withPush {
                        translate(mergedShape.blockPos, origin)
                        addShapeFaces(last().pose(), mergedShape.shape, mergedShape.key.color)
                    }
                }
            }
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val types = allTypes.filter { it.tracers && !it.color.isTransparent }
        if (types.isEmpty() && !Connections.running) return@handler

        event.renderEnvironment {
            val eyeVector = Vec3f.eyeVector(camera)
            val resolutionScale = if (scaleWithResolution) (mc.window.width / 1920F).coerceAtLeast(1F) else 1F
            val width = lineWidth * resolutionScale
            for (type in types) {
                for (blockPos in StorageScanner.iterate(type)) {
                    if (!type.shouldRender(blockPos)) continue
                    val pos = blockPos.center.subtract(camera.position()).toVec3f()
                    drawTracer(eyeVector, pos, fadedColor(type.color, blockPos.cameraDistanceSq()), width)
                }
            }

            for (entity in world.entitiesForRendering()) {
                val category = entity.categorize() ?: continue
                if (!category.shouldRender(entity) || !category.tracers || category.color.isTransparent) continue
                val pos = entity.interpolateCurrentPosition(event.partialTicks)
                    .add(0.0, entity.bbHeight / 2.0, 0.0).subtract(camera.position()).toVec3f()
                drawTracer(eyeVector, pos, fadedColor(category.color, entity.position().cameraDistanceSq()), width)
            }

            if (Connections.running) Connections.draw(this, width)
        }
    }

    private fun WorldRenderEnvironment.drawTracer(start: Vec3f, end: Vec3f, color: Color4b, width: Float) {
        if (color.isTransparent || (end - start).lengthSqr() < 1E-6F) return
        if (tracerStyle == TracerStyle.ELBOW) {
            val points = roundedElbow(start.toVec3d(), end.toVec3d()).map(::Vec3f)
            drawLinesWithWidth(color.argb, width, *points.toTypedArray())
        } else {
            drawLinesWithWidth(color.argb, width, start, end)
        }
    }

    private object StorageTopology {
        private val dirty = atomic(true)
        private var previousBudget = 0
        @Volatile
        var graph = StorageTopologyGraph(emptyList())
            private set

        fun markDirty() { dirty.value = true }
        fun clear() {
            graph = StorageTopologyGraph(emptyList())
            dirty.value = true
        }

        fun refresh() {
            if (!Connections.running && !motion.needsTopology) return
            val budget = StorageEspCapacity.topologyNodes
            val changed = dirty.getAndSet(false)
            if (previousBudget == budget && !changed) return
            previousBudget = budget
            val states = StorageScanner.iterate().filter { (pos, type) ->
                !type.color.isTransparent && type.shouldRender(pos, ignoreDistance = true)
            }.take(budget).associate { it.key to world.getBlockState(it.key) }
            graph = buildStorageTopology(states) { pos ->
                pos in states && world.getBlockEntity(pos) is Container
            }
        }
    }

    private object Connections : ToggleableValueGroup(ModuleStorageESP, "Connections", false) {
        private val hoppers by boolean("Hoppers", true)
        private val intakes by boolean("Intakes", true)
        private val doubleChests by boolean("DoubleChests", true)
        private val routeMarkers by boolean("RouteMarkers", true)
        private val markerLimit by int("MarkerLimit", 1024, 0..8192)

        fun draw(environment: WorldRenderEnvironment, width: Float) = with(environment) {
            var markers = 0
            val now = System.nanoTime() / 1E9
            for (link in StorageTopology.graph.links) {
                val color = colorFor(link) ?: continue
                val start = link.from.center.subtract(camera.position()).toVec3f()
                val end = link.to.center.subtract(camera.position()).toVec3f()
                drawLinesWithWidth(color.argb, width, start, end)
                if (link.kind != StorageLinkKind.DOUBLE_CHEST) {
                    val direction = (end - start).normalized()
                    val side = if (direction.y != 0F) Vec3f.X_AXIS else Vec3f.Y_AXIS
                    val arrow = end - direction * 0.2F
                    drawLinesWithWidth(color.argb, width, end, arrow + side * 0.1F, end, arrow - side * 0.1F)
                    if (routeMarkers && markers < markerLimit) {
                        drawRouteMarker(link, start, end, color, width, now)
                        markers++
                    }
                }
            }
        }

        private fun WorldRenderEnvironment.drawRouteMarker(
            link: StorageLink, start: Vec3f, end: Vec3f, color: Color4b, width: Float, now: Double,
        ) {
            val marker = storageRouteMarker(now, link.from.asLong(), link.to.asLong())
            val direction = (end - start).normalized()
            val side = if (direction.y != 0F) Vec3f.X_AXIS else Vec3f.Y_AXIS
            val center = start + (end - start) * marker.progress.toFloat()
            val front = center + direction * 0.12F
            val back = center - direction * 0.12F
            val left = center + side * 0.065F
            val right = center - side * 0.065F
            val markerColor = color.with(a = (color.a * marker.opacity).toInt())
            if (!markerColor.isTransparent) {
                drawLinesWithWidth(markerColor.argb, width * 1.4F,
                    front, left, left, back, back, right, right, front)
            }
        }

        private fun colorFor(link: StorageLink): Color4b? {
            if (!visible(link.kind)) return null
            val type = StorageScanner.typeAt(link.from) ?: return null
            val targetType = StorageScanner.typeAt(link.to) ?: return null
            if (!type.shouldRender(link.from) || !targetType.shouldRender(link.to)) return null
            if (type.color.isTransparent || targetType.color.isTransparent) return null
            return fadedColor(type.color, maxOf(link.from.cameraDistanceSq(), link.to.cameraDistanceSq()))
        }

        private fun visible(kind: StorageLinkKind) = when (kind) {
            StorageLinkKind.OUTPUT -> hoppers
            StorageLinkKind.INTAKE -> intakes
            StorageLinkKind.OUTPUT_AND_INTAKE -> hoppers || intakes
            StorageLinkKind.DOUBLE_CHEST -> doubleChests
        }
    }

    @JvmStatic
    fun Entity?.categorize(): ChestType? {
        return when (this) {
            // This includes any storage type minecart entity including ChestMinecartEntity
            is MinecartHopper -> ChestType.Hopper
            is AbstractMinecartContainer -> ChestType.Chest
            is ChestBoat -> ChestType.Chest
            is ChestRaft -> ChestType.Chest
            is AbstractChestedHorse -> ChestType.Chest.takeIf { hasChest() }
            else -> null
        }
    }

    @JvmStatic
    fun BlockEntity?.categorize(): ChestType? {
        return when (this) {
            is ChestBlockEntity -> if (blockState.`is`(Blocks.TRAPPED_CHEST)) {
                ChestType.TrappedChest
            } else {
                ChestType.Chest
            }
            is BarrelBlockEntity -> ChestType.Barrel
            is EnderChestBlockEntity -> ChestType.EnderChest
            is AbstractFurnaceBlockEntity -> ChestType.Furnace
            is BrewingStandBlockEntity -> ChestType.BrewingStand
            is DispenserBlockEntity -> ChestType.Dispenser
            is CrafterBlockEntity -> ChestType.Dispenser
            is HopperBlockEntity -> ChestType.Hopper
            is ShulkerBoxBlockEntity -> ChestType.ShulkerBox
            is DecoratedPotBlockEntity -> ChestType.Pot
            is ChiseledBookShelfBlockEntity -> ChestType.Bookshelf
            is ShelfBlockEntity -> ChestType.Shelf
            else -> null
        }
    }

    private data class StorageStyle(val type: ChestType, val color: Color4b)

    private inline fun forEachTrackedBlockShapes(
        skipWhen: (BlockState) -> Boolean = { false },
        block: (blockPos: BlockPos, type: ChestType, outlineShape: VoxelShape) -> Unit,
    ) {
        for ((blockPos, type) in StorageScanner.iterate()) {
            if (type.color.isTransparent || !type.shouldRender(blockPos, ignoreDistance = true)) continue

            val blockState = world.getBlockState(blockPos)

            if (blockState.isAir) continue
            val outlineShape = blockState.getShape(world, blockPos)
            StorageScanner.cacheShape(blockPos, type, outlineShape)
            if (!skipWhen(blockState)) block(blockPos, type, outlineShape)
        }
    }

    private inline fun collectTrackedBlockShapes(
        skipWhen: (BlockState) -> Boolean = { false },
    ): List<PositionedVoxelShape<StorageStyle>> {
        val shapes = buildList {
            forEachTrackedBlockShapes(skipWhen) { blockPos, type, outlineShape ->
                add(
                    PositionedVoxelShape(
                        blockPos = blockPos.asLong(),
                        key = StorageStyle(type, type.color.fade(
                            if (motion.running) blockAnimations.opacity(blockPos.asLong()).toFloat() else 1F
                        )),
                        shape = outlineShape,
                    )
                )
            }
            addAll(RememberedShapes.collect())
        }

        val coherent = if (joinDoubleChests) mergeStorageDoubleChests(shapes, world::getBlockState) else shapes
        return if (mergeAdjacent) coherent.mergeAdjacentVoxelShapes() else coherent
    }

    private object RememberedShapes {
        fun collect(): List<PositionedVoxelShape<StorageStyle>> {
            val outgoing = if (motion.running) blockAnimations.outgoing() else emptyList()
            val outgoingKeys = outgoing.mapTo(HashSet()) { it.key }
            val memories = StorageScanner.memories
            val rememberedKeys = memories.mapTo(HashSet()) { it.blockPos }
            val remembered = memories.asSequence().filter { it.blockPos !in outgoingKeys }
                .mapNotNull { styled(it, 1.0 / 3.0) }
            val fading = outgoing.asSequence().mapNotNull {
                val target = if (it.key in rememberedKeys) 1.0 / 3.0 else 0.0
                val progress = if (it.startOpacity > 0.0) it.opacity / it.startOpacity else 0.0
                val opacity = target + (it.startOpacity - target) * progress
                styled(it.snapshot, opacity)
            }
            return (remembered + fading).toList()
        }

        private fun styled(snapshot: PositionedVoxelShape<ChestType>, opacity: Double)
            : PositionedVoxelShape<StorageStyle>? {
            val type = snapshot.key
            if (type.color.isTransparent || !type.shouldRender(BlockPos.of(snapshot.blockPos), ignoreDistance = true)) {
                return null
            }
            return PositionedVoxelShape(snapshot.blockPos, StorageStyle(type, type.color.fade(opacity.toFloat())),
                snapshot.shape)
        }
    }

    private fun markDirtyForModes(topology: Boolean = true) {
        if (topology) StorageTopology.markDirty()
        GlowMode.markDirty()
        BoxMode.markDirty()
    }

    private object StorageScanner : ChunkScanner.BlockChangeSubscriber {
        private val positions = ConcurrentHashMap<BlockPos, ChestType>()
        private val shapes = ConcurrentHashMap<Long, PositionedVoxelShape<ChestType>>()
        private val chunks = HashMap<Long, MutableSet<BlockPos>>()
        // Bounded, session-only storage snapshots. Never retain mutable entities or entire chunks.
        private val remembered = StorageEspMemory<PositionedVoxelShape<ChestType>>(
            32768, 1_800_000_000_000L,
            bucketForKey = { ChunkPos.pack(BlockPos.getX(it) shr 4, BlockPos.getZ(it) shr 4) },
        )

        override val shouldCallRecordBlockOnChunkUpdate = false

        val isEmpty get() = positions.isEmpty() && remembered.isEmpty && !blockAnimations.hasOutgoing
        fun typeAt(pos: BlockPos) = positions[pos]
        fun iterate() = positions.entries.asSequence()
        fun iterate(type: ChestType) = positions.entries.asSequence().filter { it.value == type }.map { it.key }
        val memories get() = remembered.snapshot()
        fun expireMemories(): Boolean {
            val resized = remembered.reconfigure(StorageEspCapacity.retainedBlocks,
                StorageEspCapacity.retentionMinutes * 60_000_000_000L)
            return remembered.expire() || resized
        }

        @Synchronized
        fun cacheShape(pos: BlockPos, type: ChestType, shape: VoxelShape) {
            if (positions[pos] != type) return
            val snapshot = PositionedVoxelShape(pos.asLong(), type, shape)
            if (motion.running) blockAnimations.observe(pos.asLong(), snapshot)
            if (rememberUnloaded) shapes[pos.asLong()] = snapshot
        }

        @Synchronized
        fun forgetUnloaded() {
            shapes.clear()
            remembered.clear()
            blockAnimations.clear()
            markDirtyForModes()
        }

        @Synchronized
        override fun chunkUpdate(chunk: LevelChunk) {
            // A fresh server snapshot replaces all remembered data, including removed containers.
            remembered.removeBucket(chunk.pos.pack())
            for ((pos, blockEntity) in chunk.blockEntities) {
                blockEntity.categorize()?.let { track(pos, it) }
            }
            markDirtyForModes()
        }

        @Synchronized
        override fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean) {
            val type = if (state.hasBlockEntity()) mc.level?.getBlockEntity(pos).categorize() else null
            val removedMemory = remembered.remove(pos.asLong())
            if (type == null && !positions.containsKey(pos)) {
                if (removedMemory) markDirtyForModes(topology = false)
                return
            }
            if (type == null) blockAnimations.hide(pos.asLong())
            positions.remove(pos)
            val chunkKey = ChunkPos.pack(pos.x shr 4, pos.z shr 4)
            chunks[chunkKey]?.let { keys ->
                keys.remove(pos)
                if (keys.isEmpty()) chunks.remove(chunkKey)
            }
            shapes.remove(pos.asLong())
            if (type != null) track(pos, type)
            // Shape changes matter even when the storage category stays the same (e.g. double chests).
            markDirtyForModes()
        }

        private fun track(pos: BlockPos, type: ChestType) {
            val immutable = pos.immutable()
            positions[immutable] = type
            chunks.getOrPut(ChunkPos.pack(pos.x shr 4, pos.z shr 4), ::HashSet).add(immutable)
        }

        @Synchronized
        override fun clearChunk(pos: ChunkPos) {
            val contained = chunks.remove(pos.pack()) ?: return
            for (block in contained) {
                positions.remove(block)
                val key = block.asLong()
                blockAnimations.hide(key)
                val shape = shapes.remove(key)
                if (rememberUnloaded && shape != null) remembered.put(key, shape)
            }
            markDirtyForModes()
        }

        @Synchronized
        override fun clearAllChunks() {
            positions.clear()
            chunks.clear()
            StorageTopology.clear()
            forgetUnloaded()
        }
    }

    override val running: Boolean
        get() {
            if (requiresChestStealer && !ModuleChestStealer.running) {
                return false
            }

            return super.running
        }

    fun showTracers() : Boolean {
        return this.running && allTypes.any { it.tracers }
    }
}

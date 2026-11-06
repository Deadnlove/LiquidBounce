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
package net.ccbluex.gratefuldead.features.module.modules.world

import net.ccbluex.gratefuldead.event.events.GameTickEvent
import net.ccbluex.gratefuldead.event.events.NotificationEvent
import net.ccbluex.gratefuldead.event.events.OverlayRenderEvent
import net.ccbluex.gratefuldead.event.events.WorldChangeEvent
import net.ccbluex.gratefuldead.event.events.WorldRenderEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.features.module.modules.render.StorageEspFocusLabel
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.render.esp.EspVisualStyle
import net.ccbluex.gratefuldead.render.esp.drawEspOutline
import net.ccbluex.gratefuldead.render.gui.indicator.IndicatorRenderer
import net.ccbluex.gratefuldead.render.renderEnvironment
import net.ccbluex.gratefuldead.utils.block.ChunkScanner
import net.ccbluex.gratefuldead.utils.client.notification
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.function.Predicate
import kotlin.math.floor

/** Ranks visible evidence; generated structures can contain the same blocks. */
object ModuleBaseFinder : ClientModule("BaseFinder", ModuleCategories.WORLD) {
    private val minSignals by int("MinSignals", 4, 1..64)
    private val minCategories by int("MinCategories", 2, 1..3)
    private val minScore by int("MinScore", 6, 1..20)
    private val distance by int("Distance", 256, 16..4096, "m")
    private val maxResults by int("MaxResults", 16, 1..128)
    private val announce by boolean("Notify", false)
    private val color by color("Color", Color4b(183, 238, 99, 220))
    private val index = BaseFinderEvidenceIndex()
    private var results = emptyList<BaseFinding>()
    internal val hasNearbyFinding: Boolean get() = running && results.isNotEmpty()
    private val announced = HashSet<Long>()

    override fun onEnabled() {
        // An out-of-world toggle may have left the previous subscription registered.
        ChunkScanner.unsubscribe(Scanner)
        ChunkScanner.subscribe(Scanner)
    }
    override fun onDisabled() {
        ChunkScanner.unsubscribe(Scanner)
        results = emptyList()
        announced.clear()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        results = emptyList()
        announced.clear()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val maxDistanceSq = distance.toDouble() * distance
        val candidates = index.snapshot().filter {
            it.qualifies(minSignals, minCategories, minScore) && player.distanceToSqr(it.x, it.y, it.z) <= maxDistanceSq
        }.sortedWith(compareByDescending<BaseFinding> { it.score }.thenBy { player.distanceToSqr(it.x, it.y, it.z) })
        results = candidates.take(maxResults)
        val live = candidates.mapTo(HashSet()) { it.anchor.pack() }
        announced.retainAll(live)
        if (announce) {
            results.firstOrNull { it.anchor.pack() !in announced }?.let {
                announced.add(it.anchor.pack())
                val location = "${floor(it.x).toInt()}, ${floor(it.y).toInt()}, ${floor(it.z).toInt()}"
                notification(name, "Possible base near $location · ${it.count} signs", NotificationEvent.Severity.INFO)
            }
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {
            for (finding in results) {
                val pos = Vec3(finding.x, finding.y, finding.z).subtract(camera.position())
                drawEspOutline(AABB(pos.x - 0.5, pos.y - 0.5, pos.z - 0.5,
                    pos.x + 0.5, pos.y + 0.5, pos.z + 0.5), color, EspVisualStyle.lineWidth)
            }
        }
    }

    @Suppress("unused")
    private val overlayHandler = handler<OverlayRenderEvent> {
        for (finding in results) {
            val text = "Possible base · Storage ${finding.storage} · Work ${finding.workstation}" +
                " · Redstone ${finding.redstone}"
            IndicatorRenderer.submit(StorageEspFocusLabel(Vec3(finding.x, finding.y + 0.8, finding.z), color, text))
        }
    }

    private object Scanner : ChunkScanner.BlockChangeSubscriber, Predicate<BlockState> {
        override val shouldCallRecordBlockOnChunkUpdate = false
        override fun test(state: BlockState) = baseEvidence(state) != null

        override fun chunkUpdate(chunk: LevelChunk) {
            val matches = LinkedHashMap<Long, BaseEvidence>()
            chunk.findBlocks(this) { pos, state ->
                if (matches.size < 256) baseEvidence(state)?.let { matches[pos.asLong()] = it }
            }
            index.replace(chunk.pos, matches)
        }
        override fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean) =
            index.update(pos, baseEvidence(state))
        override fun clearChunk(pos: ChunkPos) = index.remove(pos)
        override fun clearAllChunks() = index.clear()
    }
}

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

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ShulkerBoxBlock
import net.minecraft.world.level.block.state.BlockState

internal enum class BaseEvidence { STORAGE, WORKSTATION, REDSTONE }

/** These blocks are evidence only: several also occur in generated structures. */
internal fun baseEvidence(state: BlockState): BaseEvidence? = when (state.block) {
    Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.ENDER_CHEST -> BaseEvidence.STORAGE
    is ShulkerBoxBlock -> BaseEvidence.STORAGE
    Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.BREWING_STAND,
    Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.BEACON ->
        BaseEvidence.WORKSTATION
    Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER, Blocks.OBSERVER, Blocks.PISTON, Blocks.STICKY_PISTON,
    Blocks.REDSTONE_WIRE, Blocks.REDSTONE_BLOCK, Blocks.REPEATER, Blocks.COMPARATOR -> BaseEvidence.REDSTONE
    else -> null
}

internal data class BaseFinding(
    val anchor: ChunkPos,
    val x: Double,
    val y: Double,
    val z: Double,
    val storage: Int,
    val workstation: Int,
    val redstone: Int,
) {
    val count get() = storage + workstation + redstone
    val categories get() = listOf(storage, workstation, redstone).count { it > 0 }
    // Saturation prevents one massive pile of a single block from dominating the ranking.
    val score get() = minOf(storage, 4) * 2 + minOf(workstation, 3) * 2 + minOf(redstone, 6)
    fun qualifies(minCount: Int, minCategories: Int, minScore: Int) =
        count >= minCount && categories >= minCategories && score >= minScore
}

/** Bounded session-only evidence, updated by the ordered chunk scanner. */
internal class BaseFinderEvidenceIndex(
    private val maxChunks: Int = 4096,
    private val maxPerChunk: Int = 256,
) {
    private val chunks = LinkedHashMap<Long, MutableMap<Long, BaseEvidence>>()
    private var dirty = true
    private var cached = emptyList<BaseFinding>()

    init { require(maxChunks > 0 && maxPerChunk > 0) }

    @Synchronized
    fun replace(chunk: ChunkPos, states: Map<Long, BaseEvidence>) {
        chunks.remove(chunk.pack())
        if (states.isNotEmpty()) {
            chunks[chunk.pack()] = states.entries.take(maxPerChunk).associateTo(LinkedHashMap()) { it.toPair() }
            while (chunks.size > maxChunks) chunks.remove(chunks.keys.first())
        }
        dirty = true
    }

    @Synchronized
    fun update(pos: BlockPos, evidence: BaseEvidence?) {
        val chunk = ChunkPos.pack(pos.x shr 4, pos.z shr 4)
        val entries = chunks[chunk]
        val key = pos.asLong()
        if (evidence == null) {
            if (entries?.remove(key) != null) {
                if (entries.isEmpty()) chunks.remove(chunk)
                dirty = true
            }
        } else {
            if (entries == null) {
                replace(ChunkPos(pos.x shr 4, pos.z shr 4), mapOf(key to evidence))
            } else if (entries.containsKey(key) || entries.size < maxPerChunk) {
                if (entries.put(key, evidence) != evidence) dirty = true
            }
        }
    }

    @Synchronized
    fun remove(chunk: ChunkPos) {
        if (chunks.remove(chunk.pack()) != null) dirty = true
    }

    @Synchronized
    fun clear() { chunks.clear(); cached = emptyList(); dirty = false }

    @Synchronized
    fun snapshot(): List<BaseFinding> {
        if (dirty) {
            cached = buildFindings(chunks)
            dirty = false
        }
        return cached
    }
}

private fun buildFindings(chunks: Map<Long, Map<Long, BaseEvidence>>): List<BaseFinding> {
    val summaries = chunks.mapValues { (key, entries) -> summarize(ChunkPos.unpack(key), entries) }
    val pending = summaries.keys.toMutableSet()
    val findings = mutableListOf<BaseFinding>()
    while (pending.isNotEmpty()) {
        val first = pending.first()
        val queue = ArrayDeque<Long>()
        val group = mutableListOf<BaseFinding>()
        queue.add(first)
        pending.remove(first)
        while (queue.isNotEmpty()) {
            val current = summaries.getValue(queue.removeFirst())
            group.add(current)
            enqueueNeighbors(current, summaries, pending, queue)
        }
        findings.add(combine(group))
    }
    return findings
}

private fun enqueueNeighbors(
    current: BaseFinding, summaries: Map<Long, BaseFinding>, pending: MutableSet<Long>, queue: ArrayDeque<Long>,
) {
    for (dx in -1..1) for (dz in -1..1) {
        val key = ChunkPos.pack(current.anchor.x + dx, current.anchor.z + dz)
        val neighbor = summaries[key] ?: continue
        if (key in pending && nearby(current, neighbor)) {
            pending.remove(key)
            queue.add(key)
        }
    }
}

private fun nearby(a: BaseFinding, b: BaseFinding): Boolean {
    val dx = a.x - b.x
    val dz = a.z - b.z
    return dx * dx + dz * dz <= 256.0 && kotlin.math.abs(a.y - b.y) <= 12.0
}

private fun summarize(chunk: ChunkPos, entries: Map<Long, BaseEvidence>): BaseFinding {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    val counts = IntArray(BaseEvidence.entries.size)
    entries.forEach { (pos, evidence) ->
        x += BlockPos.getX(pos) + 0.5
        y += BlockPos.getY(pos) + 0.5
        z += BlockPos.getZ(pos) + 0.5
        counts[evidence.ordinal]++
    }
    val size = entries.size
    return BaseFinding(chunk, x / size, y / size, z / size, counts[0], counts[1], counts[2])
}

private fun combine(group: List<BaseFinding>): BaseFinding {
    val anchor = group.minWith(compareBy<BaseFinding> { it.anchor.x }.thenBy { it.anchor.z }).anchor
    val size = group.sumOf { it.count }
    return BaseFinding(anchor, group.sumOf { it.x * it.count } / size,
        group.sumOf { it.y * it.count } / size, group.sumOf { it.z * it.count } / size,
        group.sumOf { it.storage }, group.sumOf { it.workstation }, group.sumOf { it.redstone })
}

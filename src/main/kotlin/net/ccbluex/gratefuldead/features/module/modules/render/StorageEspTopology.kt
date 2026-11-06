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

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.HopperBlock
import net.minecraft.world.level.block.state.BlockState

internal enum class StorageLinkKind { OUTPUT, INTAKE, OUTPUT_AND_INTAKE, DOUBLE_CHEST }
internal data class StorageLink(val from: BlockPos, val to: BlockPos, val kind: StorageLinkKind)

/** Snapshot of possible item routes. It contains no inventory data or simulated transfers. */
internal class StorageTopologyGraph(val links: List<StorageLink>) {
    private val neighbors = buildMap<BlockPos, MutableSet<BlockPos>> {
        links.forEach {
            getOrPut(it.from, ::LinkedHashSet).add(it.to)
            getOrPut(it.to, ::LinkedHashSet).add(it.from)
        }
    }

    fun component(root: BlockPos, allowed: Set<BlockPos>, limit: Int): Set<BlockPos> {
        if (root !in allowed || limit <= 0) return emptySet()
        val found = linkedSetOf(root)
        val pending = ArrayDeque<BlockPos>()
        pending.add(root)
        while (pending.isNotEmpty() && found.size < limit) {
            for (next in neighbors[pending.removeFirst()].orEmpty()) {
                if (next in allowed && found.add(next)) pending.add(next)
                if (found.size >= limit) break
            }
        }
        return found
    }
}

internal fun buildStorageTopology(
    states: Map<BlockPos, BlockState>,
    canTransfer: (BlockPos) -> Boolean,
): StorageTopologyGraph = StorageTopologyBuilder(states, canTransfer).build()

private class StorageTopologyBuilder(
    private val states: Map<BlockPos, BlockState>,
    private val canTransfer: (BlockPos) -> Boolean,
) {
    private val links = LinkedHashMap<Pair<BlockPos, BlockPos>, StorageLink>()

    fun build(): StorageTopologyGraph {
        states.forEach { (pos, state) -> append(pos, state) }
        return StorageTopologyGraph(links.values.toList())
    }

    private fun append(pos: BlockPos, state: BlockState) {
        val target = storageConnectionTarget(pos, state, true, true)
        if (state.block is HopperBlock) {
            if (!state.getValue(HopperBlock.ENABLED)) return
            if (target != null && canTransfer(target)) link(pos, target, StorageLinkKind.OUTPUT)
            val above = pos.above()
            if (canTransfer(above)) link(above, pos, StorageLinkKind.INTAKE)
        } else if (target != null && states[target]?.let { isStorageDoubleChestPair(pos, state, target, it) } == true) {
            link(pos, target, StorageLinkKind.DOUBLE_CHEST)
        }
    }

    private fun link(from: BlockPos, to: BlockPos, kind: StorageLinkKind) {
        if (from !in states || to !in states) return
        val key = from to to
        val previous = links[key]
        val combined = if (previous != null && previous.kind != kind) StorageLinkKind.OUTPUT_AND_INTAKE else kind
        links[key] = StorageLink(from, to, combined)
    }
}

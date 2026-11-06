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

import net.ccbluex.gratefuldead.utils.math.PositionedVoxelShape
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.phys.shapes.Shapes

/** Physical capacity, never an estimate of contents or free space. */
internal fun storageChestLabel(state: BlockState, fallback: String): String = when {
    state.`is`(Blocks.ENDER_CHEST) -> "Ender chest · 27 slots"
    state.block is ChestBlock -> {
        val trapped = state.`is`(Blocks.TRAPPED_CHEST)
        val double = state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
        val name = if (trapped) "Trapped chest" else "Chest"
        if (double) "Double ${name.lowercase()} · 54 slots" else "$name · 27 slots"
    }
    else -> fallback
}

/** Merge only reciprocal halves, independent of the wider adjacent-storage merge option. */
internal fun <K> mergeStorageDoubleChests(
    shapes: List<PositionedVoxelShape<K>>,
    stateAt: (BlockPos) -> BlockState,
): List<PositionedVoxelShape<K>> {
    val remaining = shapes.associateBy { it.blockPos }.toMutableMap()
    return buildList {
        for (snapshot in shapes) {
            val first = remaining.remove(snapshot.blockPos) ?: continue
            val pos = BlockPos.of(first.blockPos)
            val state = stateAt(pos)
            val partner = storageConnectionTarget(pos, state, false, true)
            val second = partner?.let { remaining[it.asLong()] }
            if (partner == null || second == null || first.key != second.key) {
                add(first)
                continue
            }
            val partnerState = stateAt(partner)
            val paired = isStorageDoubleChestPair(pos, state, partner, partnerState) ||
                isStorageDoubleChestPair(partner, partnerState, pos, state)
            if (!paired) {
                add(first)
                continue
            }
            remaining.remove(second.blockPos)
            val delta = partner.subtract(pos)
            val combined = Shapes.or(first.shape, second.shape.move(delta.x.toDouble(),
                delta.y.toDouble(), delta.z.toDouble()))
            add(PositionedVoxelShape(first.blockPos, first.key, combined))
        }
    }
}

internal fun storageChestBounds(
    pos: BlockPos,
    state: BlockState,
    bounds: net.minecraft.world.phys.AABB,
    stateAt: (BlockPos) -> BlockState,
): net.minecraft.world.phys.AABB {
    val partner = storageConnectionTarget(pos, state, false, true) ?: return bounds
    val partnerState = stateAt(partner)
    val paired = isStorageDoubleChestPair(pos, state, partner, partnerState) ||
        isStorageDoubleChestPair(partner, partnerState, pos, state)
    return if (paired) bounds.minmax(bounds.move(partner.subtract(pos))) else bounds
}

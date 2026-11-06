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

import net.ccbluex.gratefuldead.utils.block.anotherChestPartDirection
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.HopperBlock
import net.minecraft.world.level.block.state.BlockState

/** Potential topology only: the server does not expose inventory contents or successful transfers. */
internal fun storageConnectionTarget(
    pos: BlockPos,
    state: BlockState,
    hopperConnections: Boolean,
    doubleChestConnections: Boolean,
): BlockPos? = when {
    state.block is HopperBlock -> if (hopperConnections && state.getValue(HopperBlock.ENABLED)) {
        pos.relative(state.getValue(HopperBlock.FACING))
    } else {
        null
    }
    doubleChestConnections -> state.anotherChestPartDirection()?.let(pos::relative)
    else -> null
}

/** Validate reciprocal halves and emit one link, even with adjacent unrelated/trapped chests. */
internal fun isStorageDoubleChestPair(
    pos: BlockPos,
    state: BlockState,
    target: BlockPos,
    targetState: BlockState,
): Boolean {
    if (pos.asLong() >= target.asLong() || state.block !is ChestBlock || state.block != targetState.block) return false
    if (state.getValue(ChestBlock.FACING) != targetState.getValue(ChestBlock.FACING)) return false
    val direction = state.anotherChestPartDirection() ?: return false
    val reverse = targetState.anotherChestPartDirection() ?: return false
    return pos.relative(direction) == target && target.relative(reverse) == pos
}

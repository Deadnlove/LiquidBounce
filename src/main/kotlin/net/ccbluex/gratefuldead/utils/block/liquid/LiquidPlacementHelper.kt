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
package net.ccbluex.gratefuldead.utils.block.liquid

import net.ccbluex.gratefuldead.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.gratefuldead.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.gratefuldead.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.gratefuldead.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.gratefuldead.utils.block.targetfinding.PlacementPlan
import net.ccbluex.gratefuldead.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.gratefuldead.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.gratefuldead.utils.client.player
import net.ccbluex.gratefuldead.utils.inventory.HotbarItemSlot
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

internal fun planPlacementAtPos(
    pos: BlockPos,
    slot: HotbarItemSlot,
    placementPlayerPos: Vec3 = player.position(),
): PlacementPlan? {
    val options = BlockPlacementTargetFindingOptions(
        BlockOffsetOptions.Default,
        FaceHandlingOptions(CenterTargetPositionFactory),
        stackToPlaceWith = slot.itemStack,
        PlayerLocationOnPlacement(position = placementPlayerPos),
    )

    val bestPlacementPlan = findBestBlockPlacementTarget(pos, options) ?: return null

    return PlacementPlan(pos, bestPlacementPlan, slot)
}

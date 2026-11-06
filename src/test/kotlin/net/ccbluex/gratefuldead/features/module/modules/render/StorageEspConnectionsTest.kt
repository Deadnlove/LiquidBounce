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

import net.ccbluex.gratefuldead.test.MinecraftBootstrap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.HopperBlock
import net.minecraft.world.level.block.state.properties.ChestType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class StorageEspConnectionsTest {
    @BeforeTest
    fun bootstrap() = MinecraftBootstrap.ensureInitialized()

    @Test
    fun `hopper output follows all five possible facing directions`() {
        val pos = BlockPos(-48, 200, 32)
        for (direction in listOf(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            val state = Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, direction)
            assertEquals(pos.relative(direction), storageConnectionTarget(pos, state, true, false))
        }
    }

    @Test
    fun `powered or hidden hoppers have no active output link`() {
        val pos = BlockPos.ZERO
        val state = Blocks.HOPPER.defaultBlockState()
        assertNull(storageConnectionTarget(pos, state.setValue(HopperBlock.ENABLED, false), true, true))
        assertNull(storageConnectionTarget(pos, state, false, true))
    }

    @Test
    fun `double chest halves link reciprocally while singles never link`() {
        val pos = BlockPos(10, 64, 10)
        for (block in listOf(Blocks.CHEST, Blocks.TRAPPED_CHEST)) {
            val single = block.defaultBlockState()
            assertNull(storageConnectionTarget(pos, single, false, true))
            val left = single.setValue(ChestBlock.TYPE, ChestType.LEFT)
            val right = single.setValue(ChestBlock.TYPE, ChestType.RIGHT)
            val target = storageConnectionTarget(pos, left, false, true)!!
            assertEquals(pos, storageConnectionTarget(target, right, false, true))
            assertNull(storageConnectionTarget(pos, left, true, false))
        }
    }

    @Test
    fun `pair validation rejects duplicate reversed mismatched and differently facing halves`() {
        val pos = BlockPos(10, 64, 10)
        val left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT)
        val right = left.setValue(ChestBlock.TYPE, ChestType.RIGHT)
        val target = storageConnectionTarget(pos, left, false, true)!!
        val forward = isStorageDoubleChestPair(pos, left, target, right)
        val reverse = isStorageDoubleChestPair(target, right, pos, left)
        assertTrue(forward != reverse)
        assertFalse(isStorageDoubleChestPair(pos, left, target,
            Blocks.TRAPPED_CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT)))
        assertFalse(isStorageDoubleChestPair(pos, left, target, right.setValue(ChestBlock.FACING, Direction.SOUTH)))
        assertFalse(isStorageDoubleChestPair(pos, left, target, left))
    }

    @Test
    fun `nearby unrelated blocks never create a connection`() {
        assertNull(storageConnectionTarget(BlockPos.ZERO, Blocks.BARREL.defaultBlockState(), true, true))
        assertNull(storageConnectionTarget(BlockPos.ZERO, Blocks.ENDER_CHEST.defaultBlockState(), true, true))
    }
}

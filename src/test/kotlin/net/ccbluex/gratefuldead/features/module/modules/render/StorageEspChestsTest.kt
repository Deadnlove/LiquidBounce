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
import net.ccbluex.gratefuldead.utils.math.PositionedVoxelShape
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.phys.shapes.Shapes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageEspChestsTest {
    @BeforeTest fun bootstrap() = MinecraftBootstrap.ensureInitialized()

    @Test
    fun `matching double halves merge in either iteration order`() {
        val pos = BlockPos(10, 64, 10)
        val left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT)
        val right = left.setValue(ChestBlock.TYPE, ChestType.RIGHT)
        val partner = storageConnectionTarget(pos, left, false, true)!!
        val states = mapOf(pos to left, partner to right)
        val shapes = listOf(pos, partner).map { PositionedVoxelShape(it.asLong(), "chest", Shapes.block()) }
        for (ordered in listOf(shapes, shapes.reversed())) {
            val merged = mergeStorageDoubleChests(ordered) { states.getValue(it) }
            assertEquals(1, merged.size)
            val box = merged.single().shape.bounds()
            assertEquals(2.0, box.xsize * box.ysize * box.zsize)
        }
    }

    @Test
    fun `unrelated singles and different styles stay separate`() {
        val pos = BlockPos.ZERO
        val shapes = listOf(pos, pos.east()).map { PositionedVoxelShape(it.asLong(), "chest", Shapes.block()) }
        assertEquals(2, mergeStorageDoubleChests(shapes) { Blocks.CHEST.defaultBlockState() }.size)
        val left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT)
        val partner = storageConnectionTarget(pos, left, false, true)!!
        val styled = listOf(PositionedVoxelShape(pos.asLong(), "blue", Shapes.block()),
            PositionedVoxelShape(partner.asLong(), "hidden", Shapes.block()))
        assertEquals(2, mergeStorageDoubleChests(styled) { left }.size)
    }

    @Test
    fun `labels distinguish private trapped and double capacities`() {
        assertEquals("Ender chest · 27 slots", storageChestLabel(Blocks.ENDER_CHEST.defaultBlockState(), ""))
        assertEquals("Trapped chest · 27 slots", storageChestLabel(Blocks.TRAPPED_CHEST.defaultBlockState(), ""))
        assertEquals("Double chest · 54 slots", storageChestLabel(Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.TYPE, ChestType.LEFT), ""))
        assertEquals("Barrel", storageChestLabel(Blocks.BARREL.defaultBlockState(), "Barrel"))
    }
}

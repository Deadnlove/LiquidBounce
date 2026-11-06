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
import net.minecraft.world.level.block.HopperBlock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageEspTopologyTest {
    @BeforeTest
    fun bootstrap() = MinecraftBootstrap.ensureInitialized()

    @Test
    fun `hopper network includes intake and output but excludes remote ender storage`() {
        val hopper = BlockPos(0, 64, 0)
        val input = hopper.above()
        val output = hopper.east()
        val states = mapOf(
            hopper to Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST),
            input to Blocks.CHEST.defaultBlockState(),
            output to Blocks.BARREL.defaultBlockState(),
        )
        val graph = buildStorageTopology(states) { it in states }
        assertEquals(setOf(StorageLink(hopper, output, StorageLinkKind.OUTPUT),
            StorageLink(input, hopper, StorageLinkKind.INTAKE)), graph.links.toSet())
        assertEquals(states.keys, graph.component(input, states.keys, 10))
        assertEquals(2, graph.component(input, states.keys, 2).size)
        assertEquals(setOf(input), graph.component(input, setOf(input, output), 10))
        assertTrue(graph.component(input, states.keys, 0).isEmpty())
        assertTrue(buildStorageTopology(states) { it == hopper }.links.isEmpty())
        val locked = states + (hopper to states.getValue(hopper).setValue(HopperBlock.ENABLED, false))
        assertTrue(buildStorageTopology(locked) { true }.links.isEmpty())
    }

    @Test
    fun `vertical hoppers emit one directed route even when both output and intake describe it`() {
        val lower = BlockPos(0, -50, 0)
        val upper = lower.above()
        val states = mapOf(lower to Blocks.HOPPER.defaultBlockState(), upper to Blocks.HOPPER.defaultBlockState())
        val graph = buildStorageTopology(states) { it in states }
        assertEquals(1, graph.links.size)
        assertEquals(StorageLinkKind.OUTPUT_AND_INTAKE, graph.links.single().kind)
        assertEquals(graph.links, buildStorageTopology(states.entries.reversed().associate { it.toPair() }) {
            it in states
        }.links)
        assertEquals(upper, graph.links.single().from)
        assertEquals(lower, graph.links.single().to)
        assertEquals(states.keys, graph.component(lower, states.keys, 10))
    }
}

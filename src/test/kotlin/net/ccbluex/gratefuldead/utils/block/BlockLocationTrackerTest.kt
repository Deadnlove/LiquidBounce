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
package net.ccbluex.gratefuldead.utils.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockLocationTrackerTest {
    private class Tracker : AbstractBlockLocationTracker.State2BlockPos<String>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): String? = null
    }

    @Test
    fun `unload clears every state bucket and preserves other chunks including negative coordinates`() {
        val tracker = Tracker()
        tracker.track(BlockPos(-17, 64, -1), "chest")
        tracker.track(BlockPos(-18, 80, -2), "hopper")
        tracker.track(BlockPos(-19, 90, -3), "barrel")
        val retained = BlockPos(-16, 64, -1)
        tracker.track(retained, "chest")
        tracker.clearChunk(ChunkPos(-2, -1))
        assertEquals(listOf(retained), tracker.allPositions().map(BlockPos::immutable).toList())
        assertEquals(emptyList(), tracker.iterate("hopper").toList())
        assertEquals(emptyList(), tracker.iterate("barrel").toList())
    }
}

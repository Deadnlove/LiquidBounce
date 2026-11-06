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

import net.ccbluex.gratefuldead.test.MinecraftBootstrap
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaseFinderEvidenceTest {
    @BeforeTest
    fun bootstrap() = MinecraftBootstrap.ensureInitialized()

    @Test
    fun `single generated chest and homogeneous storage do not qualify by default`() {
        val index = BaseFinderEvidenceIndex()
        repeat(10) { index.update(BlockPos(it, 64, 0), BaseEvidence.STORAGE) }
        val finding = index.snapshot().single()
        assertFalse(finding.qualifies(4, 2, 6))
        assertEquals(8, finding.score)
        assertNull(baseEvidence(Blocks.STONE.defaultBlockState()))
        assertEquals(BaseEvidence.STORAGE, baseEvidence(Blocks.CHEST.defaultBlockState()))
        assertEquals(BaseEvidence.WORKSTATION, baseEvidence(Blocks.FURNACE.defaultBlockState()))
        assertEquals(BaseEvidence.REDSTONE, baseEvidence(Blocks.HOPPER.defaultBlockState()))
    }

    @Test
    fun `nearby evidence crosses negative chunk borders while distant or vertical evidence stays separate`() {
        val index = BaseFinderEvidenceIndex()
        index.update(BlockPos(-1, 64, 0), BaseEvidence.STORAGE)
        index.update(BlockPos(-2, 64, 0), BaseEvidence.STORAGE)
        index.update(BlockPos(0, 64, 0), BaseEvidence.WORKSTATION)
        index.update(BlockPos(1, 64, 0), BaseEvidence.REDSTONE)
        index.update(BlockPos(16, 120, 0), BaseEvidence.STORAGE)
        val findings = index.snapshot()
        assertEquals(2, findings.size)
        val nearby = findings.single { it.count == 4 }
        assertEquals(ChunkPos(-1, 0), nearby.anchor)
        assertEquals(3, nearby.categories)
        assertEquals(7, nearby.score)
        assertTrue(nearby.qualifies(4, 2, 6))
    }

    @Test
    fun `replacements updates removals unload and reset do not retain stale evidence`() {
        val index = BaseFinderEvidenceIndex()
        val p = BlockPos(16, 50, -1)
        val chunk = ChunkPos(1, -1)
        index.update(p, BaseEvidence.STORAGE)
        index.update(p, BaseEvidence.WORKSTATION)
        assertEquals(0, index.snapshot().single().storage)
        assertEquals(1, index.snapshot().single().workstation)
        index.replace(chunk, mapOf(p.asLong() to BaseEvidence.REDSTONE))
        assertEquals(1, index.snapshot().single().redstone)
        index.update(p, null)
        assertTrue(index.snapshot().isEmpty())
        index.update(p, BaseEvidence.STORAGE)
        index.remove(chunk)
        assertTrue(index.snapshot().isEmpty())
        index.update(p, BaseEvidence.STORAGE)
        index.clear()
        assertTrue(index.snapshot().isEmpty())
    }

    @Test
    fun `memory limits apply to both chunks and evidence within a chunk`() {
        val index = BaseFinderEvidenceIndex(maxChunks = 2, maxPerChunk = 2)
        repeat(4) { index.update(BlockPos(it, 64, 0), BaseEvidence.STORAGE) }
        assertEquals(2, index.snapshot().single().count)
        index.update(BlockPos(32, 64, 0), BaseEvidence.STORAGE)
        index.update(BlockPos(64, 64, 0), BaseEvidence.STORAGE)
        assertEquals(setOf(2, 4), index.snapshot().map { it.anchor.x }.toSet())
    }
}

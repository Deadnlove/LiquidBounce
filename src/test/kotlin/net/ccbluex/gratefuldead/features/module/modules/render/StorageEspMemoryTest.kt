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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageEspMemoryTest {
    private var timestamp = 0L

    @Test
    fun `bounds retained storage and evicts oldest unload first`() {
        val memory = StorageEspMemory<String>(2, 100) { timestamp }
        memory.put(1, "first")
        memory.put(2, "second")
        memory.put(1, "refreshed")
        memory.put(3, "third")
        assertEquals(listOf("refreshed", "third"), memory.snapshot())
    }

    @Test
    fun `expires exactly at the lifetime after unload`() {
        val memory = StorageEspMemory<String>(2, 100) { timestamp }
        memory.put(1, "first")
        timestamp = 99
        memory.put(2, "second")
        assertFalse(memory.expire())
        timestamp = 100
        assertTrue(memory.expire())
        assertEquals(listOf("second"), memory.snapshot())
        timestamp = 199
        assertTrue(memory.expire())
        assertTrue(memory.snapshot().isEmpty())
    }

    @Test
    fun `fresh chunk snapshots remove stale storage including deleted blocks`() {
        val memory = StorageEspMemory<Pair<Int, String>>(8, 100, bucketForKey = { if (it < 3) 4 else 5 }) { timestamp }
        memory.put(1, 4 to "removed chest")
        memory.put(2, 4 to "replaced chest")
        memory.put(3, 5 to "other chunk")
        memory.removeBucket(4)
        assertEquals(listOf(5 to "other chunk"), memory.snapshot())
        memory.remove(3)
        assertTrue(memory.snapshot().isEmpty())
    }

    @Test
    fun `clear drops world data and snapshots remain independent`() {
        val memory = StorageEspMemory<String>(2, 100) { timestamp }
        memory.put(1, "old world")
        val snapshot = memory.snapshot()
        memory.clear()
        assertTrue(memory.snapshot().isEmpty())
        assertEquals(listOf("old world"), snapshot)
    }
    @Test
    fun `reducing capacity evicts oldest and updates expiry immediately`() {
        val memory = StorageEspMemory<String>(4, 100) { timestamp }
        repeat(4) { memory.put(it.toLong(), it.toString()) }
        assertTrue(memory.reconfigure(2, 10))
        assertEquals(listOf("2", "3"), memory.snapshot())
        timestamp = 10
        assertTrue(memory.expire())
        assertTrue(memory.isEmpty)
    }

    @Test
    fun `bucket index remains correct across refresh eviction expiry and clear`() {
        val memory = StorageEspMemory<String>(2, 10, bucketForKey = { it / 10 }) { timestamp }
        memory.put(10, "a")
        memory.put(11, "b")
        memory.put(20, "c")
        assertTrue(memory.removeBucket(1))
        assertEquals(listOf("c"), memory.snapshot())
        timestamp = 10
        assertTrue(memory.expire())
        assertFalse(memory.removeBucket(2))
        memory.put(20, "refreshed")
        memory.clear()
        assertFalse(memory.removeBucket(2))
    }

}

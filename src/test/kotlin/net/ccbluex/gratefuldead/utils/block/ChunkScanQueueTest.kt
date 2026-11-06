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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChunkScanQueueTest {
    @Test
    fun `updates and unload keep arrival order while another chunk makes progress`() = runTest {
        val queue = ChunkScanQueue(this)
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        queue.enqueue(1) { events.add("load"); release.await() }
        queue.enqueue(1) { events.add("update") }
        queue.enqueue(1) { events.add("unload") }
        val other = queue.enqueue(2) { events.add("other chunk") }
        runCurrent()
        assertTrue(other.isCompleted)
        assertEquals(listOf("load", "other chunk"), events)
        release.complete(Unit)
        runCurrent()
        assertEquals(listOf("load", "other chunk", "update", "unload"), events)
        queue.enqueue(1) { events.add("reload") }.join()
        assertEquals("reload", events.last())
    }

    @Test
    fun `world reset cancels blocked predecessors and pending tails before reusing a chunk key`() = runTest {
        val queue = ChunkScanQueue(this)
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val first = queue.enqueue(1) { release.await(); events.add("old load") }
        val last = queue.enqueue(1) { events.add("old update") }
        runCurrent()
        queue.clear()
        queue.enqueue(1) { events.add("new world") }.join()
        release.complete(Unit)
        runCurrent()
        assertTrue(first.isCancelled)
        assertTrue(last.isCancelled)
        assertEquals(listOf("new world"), events)
    }
}

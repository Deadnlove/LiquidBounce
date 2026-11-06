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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Arrival order within a chunk, parallel progress between chunks. Completed tails release their keys. */
internal class ChunkScanQueue(private val scope: CoroutineScope) {
    private val tails = HashMap<Long, Job>()
    private val pending = HashSet<Job>()

    @Synchronized
    fun enqueue(chunk: Long, block: suspend CoroutineScope.() -> Unit): Job {
        val previous = tails[chunk]
        val next = scope.launch(start = CoroutineStart.LAZY) {
            previous?.join()
            ensureActive()
            block()
        }
        tails[chunk] = next
        pending.add(next)
        next.invokeOnCompletion {
            synchronized(this) {
                pending.remove(next)
                if (tails[chunk] === next) tails.remove(chunk)
            }
        }
        next.start()
        return next
    }

    @Synchronized
    fun clear() {
        // Completion handlers can remove entries; iterate a snapshot.
        val cancelled = pending.toList()
        tails.clear()
        pending.clear()
        cancelled.forEach(Job::cancel)
    }
}

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

/** Expiry visits only the oldest entries; chunk replacement uses a bucket index. */
internal class StorageEspMemory<T>(
    private var capacity: Int,
    private var lifetimeNanos: Long,
    private val bucketForKey: (Long) -> Long = { 0L },
    private val now: () -> Long = System::nanoTime,
) {
    private data class Entry<T>(val value: T, val unloadedAt: Long, val bucket: Long)
    private val entries = LinkedHashMap<Long, Entry<T>>()
    private val buckets = HashMap<Long, MutableSet<Long>>()

    init { require(capacity > 0 && lifetimeNanos > 0) }

    val isEmpty: Boolean get() = synchronized(this) { entries.isEmpty() }

    @Synchronized
    fun put(key: Long, value: T) {
        remove(key)
        val bucket = bucketForKey(key)
        entries[key] = Entry(value, now(), bucket)
        buckets.getOrPut(bucket, ::HashSet).add(key)
        while (entries.size > capacity) remove(entries.keys.first())
    }

    @Synchronized
    fun reconfigure(capacity: Int, lifetimeNanos: Long): Boolean {
        require(capacity > 0 && lifetimeNanos > 0)
        this.capacity = capacity
        this.lifetimeNanos = lifetimeNanos
        val resized = entries.size > capacity
        while (entries.size > capacity) remove(entries.keys.first())
        return resized
    }

    @Synchronized
    fun expire(): Boolean {
        val timestamp = now()
        var changed = false
        while (true) {
            val first = entries.entries.firstOrNull() ?: return changed
            if (timestamp - first.value.unloadedAt < lifetimeNanos) return changed
            remove(first.key)
            changed = true
        }
    }

    @Synchronized
    fun snapshot(): List<T> = entries.values.map { it.value }

    @Synchronized
    fun remove(key: Long): Boolean {
        val entry = entries.remove(key) ?: return false
        buckets[entry.bucket]?.let { keys ->
            keys.remove(key)
            if (keys.isEmpty()) buckets.remove(entry.bucket)
        }
        return true
    }

    @Synchronized
    fun removeBucket(bucket: Long): Boolean {
        val keys = buckets.remove(bucket) ?: return false
        keys.forEach(entries::remove)
        return true
    }

    @Synchronized
    fun clear() { entries.clear(); buckets.clear() }
}

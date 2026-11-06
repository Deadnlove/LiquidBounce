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

/** Snapshot-only fades; never keep a world, block entity, or chunk alive. */
internal class StorageEspBlockAnimation<T>(
    private val capacity: Int,
    private val duration: () -> Double,
    private val now: () -> Double = { System.nanoTime() / 1E9 },
) {
    private class Entry<T>(var snapshot: T, val fade: StorageEspTransition, var live: Boolean = true) {
        var settled = false
        var startOpacity = 1.0
    }
    data class Outgoing<T>(val key: Long, val snapshot: T, val opacity: Double, val startOpacity: Double)
    private val entries = LinkedHashMap<Long, Entry<T>>()

    val hasOutgoing: Boolean
        get() = synchronized(this) { entries.values.any { !it.live } }

    @Synchronized
    fun observe(key: Long, snapshot: T) {
        val entry = entries[key] ?: run {
            // When the animation budget is full, remaining live storage keeps normal visibility.
            if (entries.size >= capacity) return
            Entry(snapshot, StorageEspTransition(duration())).also { entries[key] = it }
        }
        entry.snapshot = snapshot
        if (!entry.live) entry.settled = false
        entry.live = true
        entry.fade.show(true, now())
    }

    @Synchronized
    fun hide(key: Long) {
        entries[key]?.let {
            if (it.live) it.startOpacity = it.fade.sample(now())
            it.live = false
            it.settled = false
            it.fade.show(false, now())
        }
    }

    @Synchronized
    fun opacity(key: Long): Double = entries[key]?.fade?.sample(now()) ?: 1.0

    @Synchronized
    fun outgoing(): List<Outgoing<T>> = entries.mapNotNull { (key, value) ->
        if (value.live) null else Outgoing(key, value.snapshot, value.fade.sample(now()), value.startOpacity)
    }

    @Synchronized
    fun advance(): Boolean {
        val timestamp = now()
        var changed = false
        entries.values.removeIf {
            val opacity = it.fade.sample(timestamp)
            changed = changed || !it.settled
            it.settled = it.live && opacity >= 1.0
            !it.live && opacity <= 0.0
        }
        return changed
    }

    @Synchronized
    fun clear() = entries.clear()
}

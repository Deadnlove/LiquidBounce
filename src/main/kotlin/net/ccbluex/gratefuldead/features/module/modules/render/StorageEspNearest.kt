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

import java.util.PriorityQueue

/** Keep at most [limit] candidates, retaining iteration order for equal distances. */
internal fun <T> nearestStorageCandidates(items: Sequence<T>, limit: Int, distance: (T) -> Double): List<T> {
    if (limit <= 0) return emptyList()
    data class Candidate<T>(val item: T, val distance: Double, val order: Long)
    val nearestFirst = compareBy<Candidate<T>> { it.distance }.thenBy { it.order }
    val farthestFirst = PriorityQueue(nearestFirst.reversed())
    var order = 0L
    for (item in items) {
        val candidate = Candidate(item, distance(item), order++)
        if (farthestFirst.size < limit) {
            farthestFirst.add(candidate)
        } else if (nearestFirst.compare(candidate, farthestFirst.peek()) < 0) {
            farthestFirst.poll()
            farthestFirst.add(candidate)
        }
    }
    return farthestFirst.sortedWith(nearestFirst).map { it.item }
}

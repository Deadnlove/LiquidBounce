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

class StorageEspNearestTest {
    @Test
    fun `nearest selection is bounded and preserves ordering at equal distances`() {
        val candidates = listOf("far" to 100.0, "first" to 1.0, "second" to 1.0, "near" to 0.5, "last" to 1.0)
        assertEquals(listOf("near", "first", "second"),
            nearestStorageCandidates(candidates.asSequence(), 3) { it.second }.map { it.first })
        assertEquals(listOf("near", "first", "second", "last", "far"),
            nearestStorageCandidates(candidates.asSequence(), 10) { it.second }.map { it.first })
        assertEquals(emptyList(), nearestStorageCandidates(candidates.asSequence(), 0) { it.second })
    }

    @Test
    fun `bounded selection matches stable full sort for a dense collection`() {
        val candidates = (0 until 20_000).map { it to ((it * 59) % 101).toDouble() }
        val expected = candidates.sortedBy { it.second }.take(127)
        assertEquals(expected, nearestStorageCandidates(candidates.asSequence(), 127) { it.second })
    }
}

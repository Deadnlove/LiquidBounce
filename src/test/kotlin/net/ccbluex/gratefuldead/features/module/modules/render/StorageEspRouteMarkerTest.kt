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
import kotlin.test.assertTrue
import kotlin.math.abs

class StorageEspRouteMarkerTest {
    @Test
    fun `marker moves forward and fades at the loop boundary`() {
        val start = storageRouteMarker(0.0, 0L, 0L)
        val middle = storageRouteMarker(1.25, 0L, 0L)
        val end = storageRouteMarker(2.5 - 1E-6, 0L, 0L)
        assertEquals(0.0, start.progress)
        assertEquals(0.0, start.opacity)
        assertEquals(0.5, middle.progress)
        assertEquals(1.0, middle.opacity)
        assertTrue(end.progress > middle.progress)
        assertTrue(end.opacity < 1E-6)
        assertEquals(start, storageRouteMarker(2.5, 0L, 0L))
    }

    @Test
    fun `position phases are stable and valid for negative coordinates and time`() {
        val sample = storageRouteMarker(-0.4, -451281L, 384L)
        assertEquals(sample, storageRouteMarker(-0.4, -451281L, 384L))
        assertTrue(sample.progress in 0.0..1.0)
        assertTrue(sample.opacity in 0.0..1.0)
        val later = storageRouteMarker(2.1, -451281L, 384L)
        assertTrue(abs(sample.progress - later.progress) < 1E-12)
        assertTrue(storageRouteMarker(0.0, 0L, 0L) != storageRouteMarker(0.0, 42L, 11L))
    }
}

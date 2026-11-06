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

package net.ccbluex.gratefuldead.render.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class DistanceFadeTest {
    @Test
    fun `default equal fade endpoints use defined hard cutoffs`() {
        assertEquals(1F, distanceFadeFactor(0F, 0F, 0F, 512F, 512F))
        assertEquals(1F, distanceFadeFactor(511F, 0F, 0F, 512F, 512F))
        assertEquals(0F, distanceFadeFactor(512F, 0F, 0F, 512F, 512F))
        assertEquals(0F, distanceFadeFactor(1024F, 0F, 0F, 512F, 512F))
    }

    @Test
    fun `near and far fades clamp and interpolate smoothly`() {
        assertEquals(0F, distanceFadeFactor(5F, 10F, 20F, 100F, 200F))
        assertEquals(0.5F, distanceFadeFactor(15F, 10F, 20F, 100F, 200F))
        assertEquals(1F, distanceFadeFactor(50F, 10F, 20F, 100F, 200F))
        assertEquals(0.5F, distanceFadeFactor(150F, 10F, 20F, 100F, 200F))
        assertEquals(0F, distanceFadeFactor(250F, 10F, 20F, 100F, 200F))
    }

    @Test
    fun `extended storage range fades beyond the former 512 block limit`() {
        assertEquals(1F, distanceFadeFactor(600F, 0F, 0F, 768F, 1024F))
        assertEquals(0.5F, distanceFadeFactor(896F, 0F, 0F, 768F, 1024F))
        assertEquals(0F, distanceFadeFactor(1024F, 0F, 0F, 768F, 1024F))
    }
}

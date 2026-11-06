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
import net.ccbluex.gratefuldead.render.esp.EspVisualStyle
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageEspAnimationTest {
    @Test
    fun `visibility eases in and out without popping when reversed`() {
        val fade = StorageEspTransition(1.0)
        fade.show(true, 10.0)
        assertEquals(0.0, fade.sample(10.0))
        assertEquals(0.5, fade.sample(10.5))
        fade.show(false, 10.5)
        assertEquals(0.5, fade.sample(10.5))
        assertEquals(0.25, fade.sample(11.0))
        fade.show(true, 11.0)
        assertEquals(0.25, fade.sample(11.0))
        assertEquals(1.0, fade.sample(12.0))
    }

    @Test
    fun `repeated show requests do not restart the transition`() {
        val fade = StorageEspTransition(0.25)
        fade.show(true, 0.0)
        fade.show(true, 0.1)
        assertEquals(1.0, fade.sample(0.25))
        fade.show(false, 0.25)
        assertEquals(0.0, fade.sample(0.5))
    }

    @Test
    fun `hull smoothing is consistent across display refresh rates`() {
        fun approach(frames: Int): Double {
            var position = 0.0
            repeat(frames) { position += (1.0 - position) * storageSmoothing(1.0 / frames) }
            return position
        }
        assertEquals(approach(30), approach(144), 1E-12)
        assertEquals(approach(60), approach(144), 1E-12)
        assertEquals(0.0, storageSmoothing(-1.0))
    }

    @Test
    fun `ease stays bounded at time discontinuities`() {
        assertEquals(0.0, EspVisualStyle.ease(-2.0))
        assertEquals(1.0, EspVisualStyle.ease(2.0))
        assertTrue(EspVisualStyle.ease(0.1) < 0.1)
        assertTrue(EspVisualStyle.ease(0.9) > 0.9)
    }
}

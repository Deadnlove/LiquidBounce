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

class StorageEspBlockAnimationTest {
    private var now = 0.0

    @Test
    fun `a delayed tick still rebuilds the fully visible final mesh`() {
        val animations = StorageEspBlockAnimation<String>(2, { 0.25 }, { now })
        animations.observe(1, "chest")
        assertEquals(0.0, animations.opacity(1))
        now = 1.0
        assertTrue(animations.advance())
        assertEquals(1.0, animations.opacity(1))
        assertFalse(animations.advance())
    }

    @Test
    fun `unloaded snapshots fade out without retaining live objects`() {
        val animations = StorageEspBlockAnimation<String>(2, { 1.0 }, { now })
        animations.observe(1, "shape")
        now = 1.0
        animations.advance()
        animations.hide(1)
        now = 1.5
        assertEquals(0.5, animations.outgoing().single().opacity)
        assertTrue(animations.hasOutgoing)
        now = 2.0
        assertTrue(animations.advance())
        assertFalse(animations.hasOutgoing)
        assertTrue(animations.outgoing().isEmpty())
    }

    @Test
    fun `budget overflow keeps unanimated containers visible and world clear drops ghosts`() {
        val animations = StorageEspBlockAnimation<String>(1, { 1.0 }, { now })
        animations.observe(1, "first")
        animations.observe(2, "budget overflow")
        assertEquals(1.0, animations.opacity(2))
        now = 1.0
        animations.hide(1)
        animations.clear()
        assertTrue(animations.outgoing().isEmpty())
        assertFalse(animations.advance())
    }
}

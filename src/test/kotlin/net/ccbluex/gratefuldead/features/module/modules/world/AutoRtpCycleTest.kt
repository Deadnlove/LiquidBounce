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
package net.ccbluex.gratefuldead.features.module.modules.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AutoRtpCycleTest {
    private val timing = AutoRtpTiming(AutoRtpMode.UNTIL_FOUND, 3.0, 60.0, 10.0)

    @Test
    fun `sends at configured times without requiring a teleport response`() {
        val cycle = AutoRtpCycle(100.0)
        assertEquals(AutoRtpAction.NONE, cycle.step(102.9, false, timing))
        assertEquals(AutoRtpAction.SEND, cycle.step(103.0, false, timing))
        cycle.sent(103.0)
        assertEquals(AutoRtpAction.NONE, cycle.step(162.9, false, timing))
        assertEquals(AutoRtpAction.SEND, cycle.step(163.0, false, timing))
        cycle.sent(163.0)
        assertEquals(AutoRtpAction.NONE, cycle.step(222.9, false, timing))
        assertEquals(AutoRtpAction.SEND, cycle.step(223.0, false, timing))
    }

    @Test
    fun `finder mode waits for scan delay and repeat mode ignores findings`() {
        val cycle = AutoRtpCycle(0.0)
        cycle.sent(1.0)
        assertEquals(AutoRtpAction.NONE, cycle.step(10.9, true, timing))
        assertEquals(AutoRtpAction.FOUND, cycle.step(11.0, true, timing))
        val repeat = timing.copy(mode = AutoRtpMode.REPEAT, interval = 2.0)
        assertEquals(AutoRtpAction.SEND, cycle.step(3.0, true, repeat))
        val scanLonger = timing.copy(interval = 2.0, scanDelay = 10.0)
        assertEquals(AutoRtpAction.NONE, cycle.step(9.0, false, scanLonger))
        assertEquals(AutoRtpAction.SEND, cycle.step(11.0, false, scanLonger))
    }

    @Test
    fun `far target uses horizontal radial distance from origin, including negatives and the world border`() {
        assertFalse(autoRtpReachedDistance(599_999.0, -800_000.0, 1_000_000))
        assertTrue(autoRtpReachedDistance(-600_000.0, 800_000.0, 1_000_000))
        assertTrue(autoRtpReachedDistance(-30_000_000.0, 0.0, 30_000_000))
        assertFalse(autoRtpReachedDistance(0.0, 0.0, 1_000))
    }

    @Test
    fun `server commands accept a slash and arguments but reject empty and multiline input`() {
        assertEquals("rtp", autoRtpCommand(" /rtp "))
        assertEquals("wild nether", autoRtpCommand("wild nether"))
        assertNull(autoRtpCommand(" / "))
        assertNull(autoRtpCommand("//rtp"))
        assertNull(autoRtpCommand("rtp\nsay hello"))
        assertNull(autoRtpCommand("a".repeat(256)))
    }
}

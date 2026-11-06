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

package net.ccbluex.gratefuldead.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientConfigMigrationTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `moves legacy profiles without replacing their contents`() {
        val legacy = directory.resolve("LiquidBounce").toFile().apply { mkdir() }
        legacy.resolve("profiles.json").writeText("saved profile")
        val migrated = migrateClientConfig(directory.toFile(), "GratefulDead")
        assertEquals("saved profile", migrated.resolve("profiles.json").readText())
        assertFalse(legacy.exists())
    }

    @Test
    fun `keeps both folders when destination already exists`() {
        directory.resolve("GratefulDead").toFile().mkdir()
        val legacy = directory.resolve("LiquidBounce").toFile().apply { mkdir() }
        legacy.resolve("profiles.json").writeText("old profile")
        migrateClientConfig(directory.toFile(), "GratefulDead")
        assertTrue(legacy.resolve("profiles.json").exists())
    }
}

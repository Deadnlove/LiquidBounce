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

import java.net.URLClassLoader
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.tools.ToolProvider
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClientRebrandingTest {
    @Test
    fun `Kotlin module relocation preserves binary fields and string lengths`() {
        val prefix = byteArrayOf(0, 0, 0, 3, -1, -128)
        val suffix = byteArrayOf(0, 14, -17)
        val input = prefix + "net.ccbluex.liquidbounce.cef".toByteArray() + suffix
        val expected = prefix + "net.ccbluex.gratefuldead.cef".toByteArray() + suffix
        assertContentEquals(expected, ClientRebranding.rewriteModule(input))
    }

    @Test
    fun `relocated browser classes load and retain upstream URLs`() {
        val directory = Files.createTempDirectory("rebrand-test").toFile()
        try {
            val source = directory.resolve("LiquidBounce.java")
            source.writeText("""
                package net.ccbluex.liquidbounce;
                public class LiquidBounce {
                    public static String identity() { return "LiquidBounce"; }
                    public static String upstream() { return "https://liquidbounce.net/download"; }
                    public LiquidBounce self() { return this; }
                }
            """.trimIndent())
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", directory.path, source.path))
            val input = directory.resolve("original.jar")
            ZipOutputStream(input.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("net/ccbluex/liquidbounce/LiquidBounce.class"))
                zip.write(directory.resolve("net/ccbluex/liquidbounce/LiquidBounce.class").readBytes())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("fabric.mod.json"))
                zip.write("""{"depends":{"liquidbounce":"*"},"entrypoints":{"liquidbounce":[]}}""".toByteArray())
                zip.closeEntry()
            }
            val output = directory.resolve("renamed.jar")
            ClientRebranding.rewriteJar(input, output)
            URLClassLoader(arrayOf(output.toURI().toURL()), null).use { loader ->
                val type = loader.loadClass("net.ccbluex.gratefuldead.GratefulDead")
                assertEquals("GratefulDead", type.getMethod("identity").invoke(null))
                assertEquals("https://liquidbounce.net/download", type.getMethod("upstream").invoke(null))
                assertEquals(type, type.getMethod("self").returnType)
            }
            ZipFile(output).use { zip ->
                assertNotNull(zip.getEntry("net/ccbluex/gratefuldead/GratefulDead.class"))
                assertEquals("""{"depends":{"gratefuldead":"*"},"entrypoints":{"gratefuldead":[]}}""",
                    zip.getInputStream(zip.getEntry("fabric.mod.json")).reader().readText())
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}

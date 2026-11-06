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

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Relocates the bundled upstream browser add-on to the client's renamed ABI. */
@CacheableTask
abstract class RebrandCefJar : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputJar: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @TaskAction
    fun rebrand() = ClientRebranding.rewriteJar(inputJar.get().asFile, outputJar.get().asFile)
}

internal object ClientRebranding {
    private val urls = Regex("https?://[^\\s\\\"<>]+")

    fun text(value: String): String {
        val protected = mutableListOf<String>()
        val masked = urls.replace(value) { match ->
            protected.add(match.value)
            "__UPSTREAM_URL_${protected.lastIndex}__"
        }
        var renamed = masked.replace("liquidbounce", "gratefuldead").replace("LiquidBounce", "GratefulDead").replace("liquidBounce", "gratefulDead")
        protected.forEachIndexed { index, url -> renamed = renamed.replace("__UPSTREAM_URL_${index}__", url) }
        return renamed
    }

    fun rewriteJar(input: File, output: File) {
        output.parentFile.mkdirs()
        ZipFile(input).use { source ->
            ZipOutputStream(output.outputStream().buffered()).use { destination ->
                source.entries().asSequence().sortedBy { it.name }.forEach { entry ->
                    if (entry.name.matches(Regex("META-INF/[^/]+\\.(SF|RSA|DSA)"))) return@forEach
                    val name = text(entry.name)
                    destination.putNextEntry(ZipEntry(name).apply { time = 0 })
                    if (!entry.isDirectory) {
                        val bytes = source.getInputStream(entry).use { it.readBytes() }
                        val rewritten = when {
                            name.endsWith(".class") -> rewriteClass(bytes)
                            name.endsWith(".kotlin_module") -> rewriteModule(bytes)
                            name.endsWith(".json") || name.endsWith(".accesswidener") -> text(bytes.toString(Charsets.UTF_8))
                                .toByteArray(Charsets.UTF_8)
                            else -> bytes
                        }
                        destination.write(rewritten)
                    }
                    destination.closeEntry()
                }
            }
        }
    }

    /** Kotlin module metadata is protobuf: equal-length ASCII names preserve its lengths and binary fields. */
    fun rewriteModule(bytes: ByteArray): ByteArray {
        val result = bytes.copyOf()
        for ((old, new) in listOf("liquidbounce" to "gratefuldead", "LiquidBounce" to "GratefulDead",
            "liquidBounce" to "gratefulDead")) {
            val before = old.toByteArray(Charsets.US_ASCII)
            val after = new.toByteArray(Charsets.US_ASCII)
            require(before.size == after.size)
            for (offset in 0..result.size - before.size) {
                if (before.indices.all { result[offset + it] == before[it] }) {
                    after.copyInto(result, offset)
                }
            }
        }
        return result
    }

    /** JVM constant-pool UTF-8 entries carry descriptors, class names, strings and annotation metadata. */
    fun rewriteClass(bytes: ByteArray): ByteArray {
        val result = ByteArrayOutputStream(bytes.size)
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            DataOutputStream(result).use { output ->
                val magic = input.readInt()
                require(magic == 0xCAFEBABE.toInt()) { "Invalid class file" }
                output.writeInt(magic)
                output.writeShort(input.readUnsignedShort())
                output.writeShort(input.readUnsignedShort())
                val count = input.readUnsignedShort()
                output.writeShort(count)
                var index = 1
                while (index < count) {
                    val tag = input.readUnsignedByte()
                    output.writeByte(tag)
                    when (tag) {
                        1 -> output.writeUTF(text(input.readUTF()))
                        3, 4, 9, 10, 11, 12, 17, 18 -> output.writeInt(input.readInt())
                        5, 6 -> { output.writeLong(input.readLong()); index++ }
                        7, 8, 16, 19, 20 -> output.writeShort(input.readUnsignedShort())
                        15 -> { output.writeByte(input.readUnsignedByte()); output.writeShort(input.readUnsignedShort()) }
                        else -> error("Unsupported class constant-pool tag $tag")
                    }
                    index++
                }
                input.transferTo(output)
            }
        }
        return result.toByteArray()
    }
}

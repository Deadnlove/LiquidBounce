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
package net.ccbluex.gratefuldead.gametest

import net.ccbluex.gratefuldead.config.ConfigSystem
import net.ccbluex.gratefuldead.event.EventListener
import net.ccbluex.gratefuldead.event.events.NotificationEvent
import net.ccbluex.gratefuldead.features.module.modules.world.ModuleBaseFinder
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.util.concurrent.atomic.AtomicReference

/** Exercises chunk scans and updates across a chunk boundary in a live client world. */
class BaseFinderGameTest : FabricClientGameTest {
    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleBaseFinder) }
        val received = AtomicReference<String>()
        val listener = object : EventListener {}
        val subscription = listener.on(NotificationEvent::class.java) {
            if (it.title == "BaseFinder") received.set(it.message)
        }
        try {
            context.worldBuilder().create().use { world ->
                val origin = context.fromClient { checkNotNull(it.player).blockPosition() }
                val points = listOf(
                    BlockPos(14, origin.y, 0), BlockPos(15, origin.y, 0),
                    BlockPos(16, origin.y, 0), BlockPos(17, origin.y, 0),
                )
                val names = listOf("chest", "barrel", "crafting_table", "hopper[facing=east]")
                points.zip(names).forEach { (p, block) ->
                    world.server.runCommand("setblock ${p.x} ${p.y} ${p.z} minecraft:$block")
                }
                context.waitFor({ client ->
                    val level = client.level
                    level != null && level.getBlockState(points[0]).`is`(Blocks.CHEST) &&
                        level.getBlockState(points[3]).`is`(Blocks.HOPPER)
                }, 200)
                context.onClient {
                    ModuleBaseFinder.settings.getValue("Notify").setByString("true")
                    ModuleBaseFinder.enabled = true
                }
                context.waitFor({ received.get() != null }, 200)
                check(received.get().contains("4 signs")) { "Expected a cross-chunk evidence notification" }
                context.waitTicks(20)
            }
        } finally {
            subscription.close()
            context.onClient {
                ModuleBaseFinder.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleBaseFinder, saved)
            }
        }
    }
}

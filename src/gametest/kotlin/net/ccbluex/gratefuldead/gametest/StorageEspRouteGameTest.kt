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
import net.ccbluex.gratefuldead.config.types.group.ToggleableValueGroup
import net.ccbluex.gratefuldead.features.module.modules.render.ModuleStorageESP
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.level.block.Blocks

/** Exercises the client render loop with a real, enabled hopper-to-barrel route. */
class StorageEspRouteGameTest : FabricClientGameTest {
    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleStorageESP) }
        try {
            context.worldBuilder().create().use { world ->
                val origin = context.fromClient { checkNotNull(it.player).blockPosition() }
                val hopper = origin.offset(2, 0, 0)
                val barrel = origin.offset(3, 0, 0)
                world.server.runCommand("setblock ${hopper.x} ${hopper.y} ${hopper.z} minecraft:hopper[facing=east]")
                world.server.runCommand("setblock ${barrel.x} ${barrel.y} ${barrel.z} minecraft:barrel")
                context.waitFor({ client ->
                    val level = client.level
                    level != null && level.getBlockState(hopper).`is`(Blocks.HOPPER) &&
                        level.getBlockState(barrel).`is`(Blocks.BARREL) && level.getBlockEntity(hopper) != null &&
                        level.getBlockEntity(barrel) != null
                }, 200)
                context.onClient {
                    val connections = ModuleStorageESP.settings.getValue("Connections") as ToggleableValueGroup
                    connections.asStateFlow().value.first { it.name == "Hoppers" }.setByString("true")
                    connections.asStateFlow().value.first { it.name == "RouteMarkers" }.setByString("true")
                    connections.asStateFlow().value.first { it.name == "MarkerLimit" }.setByString("128")
                    connections.enabled = true
                    ModuleStorageESP.enabled = true
                }
                context.waitTicks(60)
                check(context.fromClient { ModuleStorageESP.running }) { "StorageESP failed to stay enabled" }
            }
        } finally {
            context.onClient {
                ModuleStorageESP.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleStorageESP, saved)
            }
        }
    }
}

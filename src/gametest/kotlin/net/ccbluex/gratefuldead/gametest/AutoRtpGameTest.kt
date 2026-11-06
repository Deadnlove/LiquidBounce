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
import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.events.TransferOrigin
import net.ccbluex.gratefuldead.features.module.modules.world.ModuleAutoRtp
import net.ccbluex.gratefuldead.features.module.modules.world.ModuleBaseFinder
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Intercepts RTP locally and simulates a server teleport in the isolated test world. */
class AutoRtpGameTest : FabricClientGameTest {
    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val savedAuto = context.fromClient { ConfigSystem.serializeValueGroup(ModuleAutoRtp) }
        val savedFinder = context.fromClient { ConfigSystem.serializeValueGroup(ModuleBaseFinder) }
        val count = AtomicInteger()
        val reason = AtomicReference<String>()
        val listener = object : EventListener {}
        val packets = listener.on(PacketEvent::class.java) {
            val packet = it.packet
            if (it.origin == TransferOrigin.OUTGOING && packet is ServerboundChatCommandPacket &&
                packet.command() == "rtp") {
                count.incrementAndGet()
                it.cancelEvent()
            }
        }
        val notices = listener.on(NotificationEvent::class.java) {
            if (it.title == "AutoRTP") reason.set(it.message)
        }
        try {
            context.worldBuilder().create().use { world ->
                val y = context.fromClient { checkNotNull(it.player).blockPosition().y }
                onceAndRepeat(context, count)
                farCoordinates(context, count, reason) { world.server.runCommand("tp @a 1200 $y 0") }
                world.server.runCommand("tp @a 0 $y 0")
                context.waitFor({ client -> client.player?.x?.let { it < 1.0 } == true }, 200)
                context.waitFor({ client -> client.level?.hasChunk(2, 0) == true }, 200)
                listOf("chest", "barrel", "crafting_table", "hopper[facing=east]").forEachIndexed { index, block ->
                    world.server.runCommand("setblock ${34 + index} $y 0 minecraft:$block")
                }
                untilFound(context, count, reason) { world.server.runCommand("tp @a 32 $y 0") }
            }
        } finally {
            packets.close()
            notices.close()
            context.onClient {
                ModuleAutoRtp.enabled = false
                ModuleBaseFinder.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleAutoRtp, savedAuto)
                ConfigSystem.deserializeValueGroup(ModuleBaseFinder, savedFinder)
            }
        }
    }

    private fun onceAndRepeat(context: ClientGameTestContext, count: AtomicInteger) {
        context.onClient {
            ModuleAutoRtp.settings.getValue("Command").setByString("/rtp")
            ModuleAutoRtp.settings.getValue("StartDelay").setByString("0")
            ModuleAutoRtp.settings.getValue("Mode").setByString("Once")
            ModuleAutoRtp.enabled = true
        }
        context.waitFor({ count.get() == 1 && !ModuleAutoRtp.enabled }, 200)
        context.waitTicks(10)
        check(count.get() == 1) { "Once mode repeated the command" }
        count.set(0)
        context.onClient {
            ModuleAutoRtp.settings.getValue("Mode").setByString("Repeat")
            ModuleAutoRtp.settings.getValue("Delay").setByString("1")
            ModuleAutoRtp.enabled = true
        }
        context.waitFor({ count.get() >= 2 }, 200)
        context.onClient { ModuleAutoRtp.enabled = false }
        val stoppedAt = count.get()
        context.waitTicks(30)
        check(count.get() == stoppedAt) { "RTP continued after disabling" }
        count.set(0)
    }

    private fun farCoordinates(
        context: ClientGameTestContext, count: AtomicInteger, reason: AtomicReference<String>, teleport: () -> Unit,
    ) {
        context.onClient {
            ModuleAutoRtp.settings.getValue("Delay").setByString("60")
            ModuleAutoRtp.settings.getValue("Mode").setByString("FarCoordinates")
            ModuleAutoRtp.settings.getValue("TargetDistance").setByString("1000")
            ModuleAutoRtp.enabled = true
        }
        context.waitFor({ count.get() == 1 }, 200)
        teleport()
        context.waitFor({ !ModuleAutoRtp.enabled }, 200)
        check(reason.get()?.contains("Target distance") == true) { "Far-coordinate stop was not reported" }
        check(count.get() == 1) { "Unexpected extra RTP request" }
    }

    private fun untilFound(
        context: ClientGameTestContext, count: AtomicInteger, reason: AtomicReference<String>, teleport: () -> Unit,
    ) {
        count.set(0)
        reason.set(null)
        context.onClient {
            ModuleBaseFinder.enabled = false
            ModuleAutoRtp.settings.getValue("Mode").setByString("UntilFound")
            ModuleAutoRtp.settings.getValue("ScanDelay").setByString("1")
            ModuleAutoRtp.settings.getValue("Delay").setByString("60")
            ModuleAutoRtp.enabled = true
        }
        context.waitFor({ count.get() == 1 }, 200)
        teleport()
        context.waitFor({ reason.get()?.contains("Possible base found") == true }, 300)
        check(!context.fromClient { ModuleAutoRtp.enabled }) { "Auto RTP did not stop" }
        check(context.fromClient { ModuleBaseFinder.enabled }) { "Base Finder was not enabled" }
        context.waitTicks(20)
        check(count.get() == 1) { "RTP repeated after a finding" }
    }

}

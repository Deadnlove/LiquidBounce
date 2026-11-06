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

package net.ccbluex.gratefuldead.features.module.modules.player.nofall.modes

import net.ccbluex.gratefuldead.event.events.MovementInputEvent
import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.events.SprintEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.gratefuldead.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.gratefuldead.lang.translation
import net.ccbluex.gratefuldead.utils.client.chat
import net.ccbluex.gratefuldead.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.gratefuldead.utils.client.markAsError
import net.ccbluex.gratefuldead.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.gratefuldead.utils.math.component1
import net.ccbluex.gratefuldead.utils.math.component2
import net.ccbluex.gratefuldead.utils.math.component3
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.Vec3

/**
 * Does not work in server version 1.8 and below.
 *
 * @author jiuxian_baka
 */
internal object NoFallSpoofLanding : NoFallMode("SpoofLanding") {

    private val modification by vec3d("Modification", Vec3(1337.0, 0.0, 1337.0), useLocateButton = false)

    @Volatile
    private var prevFallDistance = 0.0

    @Volatile
    private var prevOnGround = false

    @Volatile
    private var flag = false

    override fun disable() {
        prevFallDistance = 0.0
        prevOnGround = false
        flag = false
        super.disable()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> {
        if (isOlderThanOrEqual1_8) {
            chat(markAsError(translation("gratefuldead.module.noFall.messages.spoofLanding")))
            ModuleNoFall.enabled = false
            return@handler
        }
        val packet = it.packet

        if (packet is ServerboundMovePlayerPacket) {
            if (packet.onGround && !prevOnGround && prevFallDistance >= playerSafeFallDistance) {
                flag = true
                val (dx, dy, dz) = modification
                packet.x += dx
                packet.y += dy
                packet.z += dz
                packet.onGround = false
                player.resetFallDistance()
            }

            prevOnGround = packet.onGround
            prevFallDistance = player.fallDistance
        }
    }

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = MODEL_STATE) { event ->
        if (flag && player.onGround()) {
            event.sprint = false
        }
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent> { event ->
        debugParameter("shouldJump") { flag }
        if (flag && player.onGround()) {
            event.jump = true
            flag = false
        }
    }
}

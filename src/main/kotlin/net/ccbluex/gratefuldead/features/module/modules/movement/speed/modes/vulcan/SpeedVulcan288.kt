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
package net.ccbluex.gratefuldead.features.module.modules.movement.speed.modes.vulcan

import net.ccbluex.gratefuldead.config.types.group.ModeValueGroup
import net.ccbluex.gratefuldead.event.events.GameTickEvent
import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.events.PlayerAfterJumpEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.event.sequenceHandler
import net.ccbluex.gratefuldead.event.waitTicks
import net.ccbluex.gratefuldead.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.gratefuldead.utils.entity.withStrafe
import net.ccbluex.gratefuldead.utils.math.copy
import net.ccbluex.gratefuldead.utils.math.multiply
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.effect.MobEffects
import kotlin.math.abs

/**
 * BHop Speed for Vulcan 288
 * Tested on both anticheat-test.com and loyisa.cn
 */
class SpeedVulcan288(parent: ModeValueGroup<*>) : SpeedBHopBase("Vulcan288", parent) {

    @Suppress("unused")
    private val afterJumpHandler = sequenceHandler<PlayerAfterJumpEvent> {
        val hasSpeed = (player.getEffect(MobEffects.SPEED)?.amplifier ?: 0) != 0

        player.deltaMovement = player.deltaMovement.withStrafe(speed = if (hasSpeed) 0.771 else 0.5)
        waitTicks(1)
        player.deltaMovement = player.deltaMovement.withStrafe(speed = if (hasSpeed) 0.605 else 0.31)
        waitTicks(1)
        player.deltaMovement = player.deltaMovement.withStrafe(speed = if (hasSpeed) 0.57 else 0.29)
        // does max possible motion down without introducing other issues
        player.deltaMovement = player.deltaMovement.copy(y = if (hasSpeed) -0.5 else -0.37)
        waitTicks(1)
        player.deltaMovement = player.deltaMovement.withStrafe(speed = if (hasSpeed) 0.595 else 0.27)
        waitTicks(1)
        player.deltaMovement = player.deltaMovement.withStrafe(speed = if (hasSpeed) 0.595 else 0.28)
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val hasSpeed = (player.getEffect(MobEffects.SPEED)?.amplifier ?: 0) != 0
        if (!player.onGround()) {
            if (abs(player.fallDistance) > 0 && hasSpeed) {
                player.deltaMovement = player.deltaMovement.multiply(factorX = 1.055, factorZ = 1.055)
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is ServerboundMovePlayerPacket && player.deltaMovement.y < 0) {
            packet.onGround = true
        }
    }

}

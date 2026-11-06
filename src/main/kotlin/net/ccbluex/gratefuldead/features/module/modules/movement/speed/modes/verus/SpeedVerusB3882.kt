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
package net.ccbluex.gratefuldead.features.module.modules.movement.speed.modes.verus

import net.ccbluex.gratefuldead.config.types.group.ModeValueGroup
import net.ccbluex.gratefuldead.event.events.PlayerAfterJumpEvent
import net.ccbluex.gratefuldead.event.events.PlayerMoveEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.event.tickHandler
import net.ccbluex.gratefuldead.event.waitTicks
import net.ccbluex.gratefuldead.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.gratefuldead.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.gratefuldead.utils.client.Timer
import net.ccbluex.gratefuldead.utils.entity.moving
import net.ccbluex.gratefuldead.utils.entity.withStrafe
import net.ccbluex.gratefuldead.utils.kotlin.Priority
import net.ccbluex.gratefuldead.utils.math.multiply
import net.minecraft.world.entity.MoverType

/**
 * @anticheat Verus
 * @anticheatVersion b3882
 * @testedOn eu.anticheat-test.com
 */
class SpeedVerusB3882(parent: ModeValueGroup<*>) : SpeedBHopBase("VerusB3882", parent) {

    @Suppress("unused")
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> {
        player.deltaMovement = player.deltaMovement.multiply(factorX = 1.1F, factorZ = 1.1F)
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        // Might just strafe when player controls itself
        if (event.type == MoverType.SELF && player.moving) {
            event.movement = event.movement.withStrafe(strength = 1.0)
        }
    }

    @Suppress("unused")
    private val timerHandler = tickHandler {
        Timer.requestTimerSpeed(2.0F, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        waitTicks(101)
    }

}

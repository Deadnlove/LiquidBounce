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
package net.ccbluex.gratefuldead.features.module.modules.movement

import net.ccbluex.gratefuldead.event.events.PlayerMoveEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.utils.entity.moving
import net.ccbluex.gratefuldead.utils.entity.withStrafe
import net.ccbluex.gratefuldead.utils.math.copy
import net.minecraft.world.entity.MoverType

/**
 * Strafe module
 *
 * Strafe into different directions while you're midair.
 */
object ModuleStrafe : ClientModule("Strafe", ModuleCategories.MOVEMENT) {

    private var strengthInAir by float("StrengthInAir", 1f, 0.0f..1f)
    private var strengthOnGround by float("StrengthOnGround", 1f, 0.0f..1f)

    private var strictMovement by boolean("StrictMovement", false)

    val moveHandler = handler<PlayerMoveEvent> { event ->
        // Might just strafe when player controls itself
        if (event.type == MoverType.SELF) {
            val strength = if (player.onGround()) strengthOnGround else strengthInAir

            // Don't strafe if strength is 0
            if (strength == 0f) {
                return@handler
            }

            if (player.moving) {
                event.movement = event.movement.withStrafe(strength = strength.toDouble())
            } else if (strictMovement) {
                event.movement = event.movement.copy(x = 0.0, z = 0.0)
            }
        }
    }

}

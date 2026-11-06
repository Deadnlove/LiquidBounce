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
package net.ccbluex.gratefuldead.features.module.modules.combat

import net.ccbluex.gratefuldead.config.utils.percentageChance
import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.event.tickHandler
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.gratefuldead.utils.client.Timer
import net.ccbluex.gratefuldead.utils.client.Timer.timerSpeed
import net.ccbluex.gratefuldead.utils.combat.findEnemy
import net.ccbluex.gratefuldead.utils.kotlin.Priority
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket

/**
 * TimerRange module
 *
 * Automatically speeds up when you are near an enemy.
 */

object ModuleTimerRange : ClientModule("TimerRange", ModuleCategories.COMBAT) {

    private val chance = percentageChance("Chance", 100f)
    private val timerBalanceLimit by float("TimerBalanceLimit", 20f, 0f..50f)
    private val normalSpeed by float("NormalSpeed", 0.9F, 0.1F..10F)
    private val inRangeSpeed by float("InRangeSpeed", 0.95F, 0.1F..10F)
    private val balanceLimitSpeed by float("BalanceLimitSpeed", 0.99F, 0.1F..1F)
    private val boostSpeed by float("BoostTimer", 2F, 0.1F..10F).apply { tagBy(this) }
    private val balanceRecoveryIncrement by float("BalanceRecoveryIncrement", 1f, 1f..10f)
    private val distanceToSpeedUp by float("DistanceToSpeedUp", 3.5f, 0f..10f)
    private val distanceToPause by float("DistanceToPause", 3f, 0f..10f)
    private val distanceToStartWorking by float("DistanceToStartWorking", 100f, 0f..500f)
    private val pauseOnFlag by boolean("PauseOnFlag", true)
    private val onlyOnGround by boolean("OnlyOnGround", false)

    private val requiresKillAura by boolean("RequiresKillAura", true)

    private var reachedTheLimit = false
    private var balanceTimer = 0f

    override fun onEnabled() {
        balanceTimer = timerBalanceLimit
        super.onEnabled()
    }

    val repeatable = tickHandler {
        if (onlyOnGround && !player.onGround()) return@tickHandler
        if (requiresKillAura && (!ModuleKillAura.running || ModuleKillAura.targetTracker.target == null)) {
            return@tickHandler
        }

        val newTimerSpeed = updateTimerSpeed()

        if (newTimerSpeed != null) {
            Timer.requestTimerSpeed(newTimerSpeed, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleTimerRange)
        }

        val balanceChange = timerSpeed / balanceRecoveryIncrement - 1
        if ((balanceTimer > 0 || balanceChange > 0) && (balanceTimer < timerBalanceLimit * 2 || balanceChange < 0)) {
            balanceTimer += balanceChange
        }

        if (balanceTimer <= timerBalanceLimit) {
            Timer.requestTimerSpeed(balanceLimitSpeed, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleTimerRange)
        }

        if (balanceTimer <= 0) {
            reachedTheLimit = false
        }
    }

    private fun updateTimerSpeed(): Float? {
        if (world.findEnemy(0f, distanceToPause) != null) {
            return 1.0f
        }

        if (world.findEnemy(0f, distanceToStartWorking) == null || !chance.asBoolean) {
            return null
        }

        if (world.findEnemy(0f, distanceToSpeedUp) == null) {
            return normalSpeed
        }

        return if (balanceTimer < timerBalanceLimit * 2 && !reachedTheLimit) {
            boostSpeed
        } else {
            reachedTheLimit = true

            inRangeSpeed
        }
    }

    val packetHandler = handler<PacketEvent> {
        if (it.packet is ClientboundPlayerPositionPacket && pauseOnFlag) {
            balanceTimer = timerBalanceLimit * 2
        }
        // Stops speeding up when you get flagged
    }

}

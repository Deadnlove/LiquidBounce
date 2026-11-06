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
package net.ccbluex.gratefuldead.utils.block.placer

import net.ccbluex.gratefuldead.config.types.group.ToggleableValueGroup
import net.ccbluex.gratefuldead.event.EventListener
import net.ccbluex.gratefuldead.event.events.GameTickEvent
import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.utils.aiming.NoRotationMode
import net.ccbluex.gratefuldead.utils.aiming.NormalRotationMode
import net.ccbluex.gratefuldead.utils.aiming.RotationManager
import net.ccbluex.gratefuldead.utils.aiming.utils.raytraceBox
import net.ccbluex.gratefuldead.utils.block.SwingMode
import net.ccbluex.gratefuldead.utils.client.Chronometer
import net.ccbluex.gratefuldead.utils.combat.attackEntity
import net.ccbluex.gratefuldead.utils.entity.getExplosionDamageFromEntity
import net.ccbluex.gratefuldead.utils.kotlin.Priority
import net.ccbluex.gratefuldead.utils.raytracing.isLookingAtEntity
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.world.entity.boss.enderdragon.EndCrystal

class CrystalDestroyFeature(private val eventListener: EventListener) :
    ToggleableValueGroup(eventListener, "DestroyCrystals", true) {

    private val range by float("Range", 4.5f, 1f..6f)
    private val wallRange by float("WallRange", 4.5f, 0f..6f)
    private val delay by int("Delay", 0, 0..1000, "ms")
    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE)

    private val rotationMode = modes(this, "RotationMode") {
        [
            NormalRotationMode(it, eventListener, Priority.IMPORTANT_FOR_USAGE_3),
            NoRotationMode(it, eventListener),
        ]
    }

    private val chronometer = Chronometer()

    var currentTarget: EndCrystal? = null
        private set

    /**
     * Suggests [crystal] as the crystal to destroy. The candidate is only accepted when it is reachable
     * and closer to the player's eye than the current target, so the choice does not depend on the order
     * in which blocked positions happen to be visited.
     */
    fun suggestTarget(crystal: EndCrystal) {
        val current = currentTarget
        if (current === crystal) {
            return
        }

        if (current != null && current.distanceToSqr(player.eyePosition) <= crystal.distanceToSqr(player.eyePosition)) {
            return
        }

        raytraceBox(
            player.eyePosition,
            crystal.boundingBox,
            range = range.toDouble(),
            wallsRange = wallRange.toDouble(),
        )?.let { currentTarget = crystal }
    }

    fun clearTarget() {
        currentTarget = null
    }

    val repeatable = handler<GameTickEvent> {
        val target = currentTarget ?: return@handler

        if (!chronometer.hasElapsed(delay.toLong())) {
            return@handler
        }

        if (wouldKill(target)) {
            clearTarget()
            return@handler
        }

        // find the best spot (and skip if no spot was found)
        val (rotation, _) =
            raytraceBox(
                player.eyePosition,
                target.boundingBox,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble(),
            ) ?: return@handler

        rotationMode.activeMode.rotate(rotation, isFinished = {
            isLookingAtEntity(
                toEntity = target,
                rotation = RotationManager.serverRotation,
                range = range.toDouble(),
                throughWallsRange = wallRange.toDouble()
            ) != null
        }, onFinished = {
            if (!chronometer.hasElapsed(delay.toLong())) {
                return@rotate
            }

            val target = currentTarget ?: return@rotate

            if (wouldKill(target)) {
                clearTarget()
                return@rotate
            }

            attackEntity(target, swingMode)
            chronometer.reset()
            clearTarget()
        })
    }

    /**
     * Checks whether the crystal would kill us.
     */
    private fun wouldKill(target: EndCrystal): Boolean {
        val health = player.health + player.absorptionAmount
        return health - player.getExplosionDamageFromEntity(target) <= 0f
    }

    @Suppress("unused")
    val destroyEntityHandler = handler<PacketEvent> {
        val target = currentTarget ?: return@handler

        val packet = it.packet
        if (packet is ClientboundRemoveEntitiesPacket && target.id in packet.entityIds) {
            clearTarget()
        }
    }

    /**
     * This should be called when the module using this destroyer is disabled.
     */
    fun onDisable() {
        clearTarget()
    }

}

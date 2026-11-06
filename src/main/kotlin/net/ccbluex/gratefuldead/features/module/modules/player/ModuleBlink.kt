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
package net.ccbluex.gratefuldead.features.module.modules.player

import net.ccbluex.gratefuldead.config.types.group.ToggleableValueGroup
import net.ccbluex.gratefuldead.config.types.list.Tagged
import net.ccbluex.gratefuldead.event.events.BlinkPacketEvent
import net.ccbluex.gratefuldead.event.events.GameTickEvent
import net.ccbluex.gratefuldead.event.events.NotificationEvent
import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.events.PlayerMovementTickEvent
import net.ccbluex.gratefuldead.event.events.TransferOrigin
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.event.tickHandler
import net.ccbluex.gratefuldead.features.blink.BlinkManager
import net.ccbluex.gratefuldead.features.blink.BlinkManager.Action
import net.ccbluex.gratefuldead.features.blink.BlinkManager.positions
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.gratefuldead.features.module.modules.player.ModuleBlink.dummyPlayer
import net.ccbluex.gratefuldead.utils.client.notification
import net.ccbluex.gratefuldead.utils.kotlin.EventPriorityConvention
import net.ccbluex.gratefuldead.utils.world.nextLocalEntityId
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.entity.EntityLookup
import java.util.UUID

/**
 * Blink module
 *
 * Suspends packets before they are sent to/received from the server.
 */

object ModuleBlink : ClientModule("Blink", ModuleCategories.PLAYER) {

    private val directions by multiEnumChoice("Directions", TransferOrigin.OUTGOING, canBeNone = false)

    private val dummy by boolean("Dummy", false)
    private val ambush by boolean("Ambush", false)
    private val autoDisable by boolean("AutoDisable", true)

    private object AutoResetOption : ToggleableValueGroup(this, "AutoReset", false) {
        val resetAfter by int("ResetAfter", 100, 1..1000)
        val action by enumChoice("ResetAction", ResetAction.RESET)
    }

    private var dummyPlayer: RemotePlayer? = null
    private var tickCounter = 0

    init {
        tree(AutoResetOption)
    }

    override fun onEnabled() {
        tickCounter = 0
        if (dummy) {
            val clone = RemotePlayer(world, player.gameProfile)

            clone.yHeadRot = player.yHeadRot
            clone.copyPosition(player)
            /**
             * A different UUID has to be set, to avoid [dummyPlayer] from being invisible to [player]
             * @see EntityLookup.add
             */
            clone.setUUID(UUID.randomUUID())
            clone.id = world.nextLocalEntityId()
            world.addEntity(clone)

            dummyPlayer = clone
        }
    }

    override fun onDisabled() {
        directions.forEach { BlinkManager.flush(it) }
        removeClone()
    }

    private fun removeClone() {
        val clone = dummyPlayer ?: return

        world.removeEntity(clone.id, Entity.RemovalReason.DISCARDED)
        dummyPlayer = null
    }

    val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        val packet = event.packet

        if (event.isCancelled || !directions.contains(event.origin)) {
            return@handler
        }

        if (ambush &&
            (packet is ServerboundInteractPacket
                || packet is ServerboundAttackPacket
                || packet is ServerboundSpectatorActionPacket)) {
            enabled = false
            return@handler
        }
    }

    @Suppress("unused")
    private val tickTask = handler<GameTickEvent> {
        if (ModuleAutoDodge.enabled) {
            val playerPosition = positions.firstOrNull() ?: return@handler

            if (ModuleAutoDodge.getInflictedHit(playerPosition) == null) {
                return@handler
            }

            val evadingPacket = ModuleAutoDodge.findAvoidingArrowPosition()

            // We have found no packet that avoids getting hit? Then we default to blinking.
            // AutoDoge might save the situation...
            if (evadingPacket == null) {
                notification(
                    "Blink", "Unable to evade arrow. Blinking.",
                    NotificationEvent.Severity.INFO
                )
                enabled = false
            } else if (evadingPacket.ticksToImpact != null) {
                notification("Blink", "Trying to evade arrow...", NotificationEvent.Severity.INFO)
                BlinkManager.flush(evadingPacket.idx + 1)
            } else {
                notification("Blink", "Arrow evaded.", NotificationEvent.Severity.INFO)
                BlinkManager.flush(evadingPacket.idx + 1)
            }
        }
    }

    @Suppress("unused")
    private val playerMoveHandler = handler<PlayerMovementTickEvent> {
        tickCounter++

        if (AutoResetOption.enabled && tickCounter > AutoResetOption.resetAfter) {
            tickCounter = 0
            when (AutoResetOption.action) {
                ResetAction.RESET -> BlinkManager.cancel()
                ResetAction.BLINK -> {
                    directions.forEach { BlinkManager.flush(it) }
                    dummyPlayer?.copyPosition(player)
                }
            }

            if (autoDisable) {
                notification("Blink", "Auto reset", NotificationEvent.Severity.INFO)
                enabled = false
            }
        }
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<BlinkPacketEvent> { event ->
        if (directions.contains(event.origin)) {
            event.action = Action.QUEUE
        }
    }

    enum class ResetAction(override val tag: String) : Tagged {
        RESET("Reset"),
        BLINK("Blink");
    }

    fun isDummyPlayer(entityId: Int): Boolean {
        return entityId == dummyPlayer?.id
    }
}

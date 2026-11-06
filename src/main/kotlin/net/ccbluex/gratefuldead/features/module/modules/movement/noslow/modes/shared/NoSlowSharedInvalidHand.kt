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
package net.ccbluex.gratefuldead.features.module.modules.movement.noslow.modes.shared

import net.ccbluex.gratefuldead.config.types.group.Mode
import net.ccbluex.gratefuldead.config.types.group.ModeValueGroup
import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.events.TransferOrigin
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.utils.client.NullableBypass
import net.ccbluex.gratefuldead.utils.network.sendPacketSilently
import net.ccbluex.gratefuldead.utils.kotlin.EventPriorityConvention
import net.minecraft.network.protocol.game.ServerboundUseItemPacket

internal class NoSlowSharedInvalidHand(override val parent: ModeValueGroup<*>) : Mode("InvalidHand") {

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val packet = event.packet

        if (!event.isCancelled && event.origin == TransferOrigin.OUTGOING && packet is ServerboundUseItemPacket) {
            event.cancelEvent()
            sendPacketSilently(NullableBypass.createWithNullHand(packet))
        }
    }

}

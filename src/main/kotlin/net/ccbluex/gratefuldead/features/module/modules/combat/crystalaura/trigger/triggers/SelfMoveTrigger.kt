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
package net.ccbluex.gratefuldead.features.module.modules.combat.crystalaura.trigger.triggers

import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.gratefuldead.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.gratefuldead.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.mc
import net.ccbluex.gratefuldead.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.runDestroy
import net.ccbluex.gratefuldead.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.runPlace
import net.ccbluex.gratefuldead.features.module.modules.combat.crystalaura.trigger.Trigger
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

/**
 * Runs placing when you move.
 */
object SelfMoveTrigger : Trigger("SelfMove", false) {

    @Suppress("unused")
    private val packetListener = handler<PacketEvent>(-1) { event ->
        val packet = event.packet
        if (packet !is ServerboundMovePlayerPacket) {
            return@handler
        }

        mc.execute {
            runDestroy { SubmoduleCrystalDestroyer.tick() }
            runPlace { SubmoduleCrystalPlacer.tick() }
        }
    }

}

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
package net.ccbluex.gratefuldead.features.module.modules.combat.tpaura

import net.ccbluex.gratefuldead.config.types.group.Mode
import net.ccbluex.gratefuldead.config.types.group.ModeValueGroup
import net.ccbluex.gratefuldead.event.events.GameTickEvent
import net.ccbluex.gratefuldead.event.events.WorldRenderEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.features.module.modules.combat.tpaura.modes.AStarMode
import net.ccbluex.gratefuldead.features.module.modules.combat.tpaura.modes.ImmediateMode
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.utils.aiming.RotationManager
import net.ccbluex.gratefuldead.utils.block.SwingMode
import net.ccbluex.gratefuldead.utils.clicking.Clicker
import net.ccbluex.gratefuldead.utils.client.Chronometer
import net.ccbluex.gratefuldead.utils.combat.TargetPriority
import net.ccbluex.gratefuldead.utils.combat.TargetSelector
import net.ccbluex.gratefuldead.utils.combat.attackEntity
import net.ccbluex.gratefuldead.utils.entity.rotation
import net.ccbluex.gratefuldead.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.gratefuldead.utils.render.WireframePlayer
import net.minecraft.world.phys.Vec3

object ModuleTpAura : ClientModule("TpAura", ModuleCategories.COMBAT, disableOnQuit = true) {

    private val attackRange by float("AttackRange", 4.2f, 3f..5f)

    val clicker = tree(Clicker(this, mc.options.keyAttack))
    val mode = choices("Mode", AStarMode, arrayOf(AStarMode, ImmediateMode))
    val targetSelector = tree(TargetSelector(TargetPriority.HURT_TIME))

    val stuckChronometer = Chronometer()
    var desyncPlayerPosition: Vec3? = null

    private val wireframePlayer = WireframePlayer()

    @Suppress("unused")
    private val attackRepeatable = handler<GameTickEvent> {
        val position = desyncPlayerPosition ?: player.position()

        clicker.click {
            val target = targetSelector.targets().firstOrNull {
                it.squaredBoxedDistanceTo(position) <= attackRange * attackRange
            } ?: return@click false

            attackEntity(target, SwingMode.DO_NOT_HIDE, keepSprint = true)
            true
        }
    }

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> { event ->
        wireframePlayer.pos = desyncPlayerPosition ?: return@handler
        wireframePlayer.setRotation(RotationManager.currentRotation ?: player.rotation)
        wireframePlayer.render(event, Color4b(36, 32, 147, 87), Color4b(36, 32, 147, 255))
    }

}

abstract class TpAuraMode(name: String) : Mode(name) {

    final override val parent: ModeValueGroup<TpAuraMode>
        get() = ModuleTpAura.mode

}

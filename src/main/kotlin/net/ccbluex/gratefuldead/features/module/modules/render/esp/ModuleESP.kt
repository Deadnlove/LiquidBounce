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
package net.ccbluex.gratefuldead.features.module.modules.render.esp

import net.ccbluex.gratefuldead.config.ConfigSystem
import net.ccbluex.gratefuldead.features.misc.FriendManager
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.features.module.modules.render.esp.modes.Esp2DMode
import net.ccbluex.gratefuldead.features.module.modules.render.esp.modes.EspBoxMode
import net.ccbluex.gratefuldead.features.module.modules.render.esp.modes.EspGlowMode
import net.ccbluex.gratefuldead.features.module.modules.render.esp.modes.EspLegacy2DMode
import net.ccbluex.gratefuldead.render.GenericDistanceHSBColorMode
import net.ccbluex.gratefuldead.render.GenericEntityHealthColorMode
import net.ccbluex.gratefuldead.render.GenericRainbowColorMode
import net.ccbluex.gratefuldead.render.GenericStaticColorMode
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.utils.combat.EntityTaggingManager
import net.ccbluex.gratefuldead.utils.combat.shouldBeShown
import net.ccbluex.gratefuldead.utils.entity.RenderedEntities
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

/**
 * ESP module
 *
 * Allows you to see targets through walls.
 */
object ModuleESP : ClientModule("ESP", ModuleCategories.RENDER) {

    override val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.module.esp"

    val modes = choices("Mode", EspGlowMode, arrayOf(
        EspBoxMode,
        Esp2DMode,
        EspLegacy2DMode,
//        EspOutlineMode,
        EspGlowMode
    ))

    private val colorModes = choices("ColorMode", 0) {
        arrayOf(
            GenericDistanceHSBColorMode.entity(it),
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b.WHITE.with(a = 100)),
            GenericRainbowColorMode(it)
        )
    }
    private val invisibleColor by color("Invisible", Color4b.ORANGE)
    private val friendColor by color("Friends", Color4b.GREEN)

    internal val maximumDistance by float("MaximumDistance", 128F, 1F..512F)

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
    }

    fun getColor(entity: LivingEntity): Color4b {
        if (entity.hurtTime > 0) {
            return Color4b.RED
        }

        if (entity.isInvisible) {
            return invisibleColor
        }

        if (entity is Player) {
            if (FriendManager.isFriend(entity) && friendColor.a > 0) {
                return friendColor
            }

            EntityTaggingManager.getTag(entity).color?.let { return it }
        }

        return colorModes.activeMode.getColor(entity)
    }

    /**
     * Check if the entity requires true sight to be shown with the current ESP mode
     */
    fun requiresTrueSight(entity: LivingEntity) =
        modes.activeMode.requiresTrueSight && entity.shouldBeShown()

}

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

package net.ccbluex.gratefuldead.render.utils

import com.mojang.renderpearl.api.commands.RenderPass
import net.ccbluex.gratefuldead.config.types.Value
import net.ccbluex.gratefuldead.config.types.group.ValueGroup
import net.ccbluex.gratefuldead.render.ClientUniformDefine
import net.ccbluex.gratefuldead.utils.render.writeStd140

class DistanceFadeUniformValueGroup(
    maxDistance: Float = 512F,
    defaultFarStart: Float = 512F,
    defaultFarEnd: Float = 512F,
) : ValueGroup("DistanceFade") {

    val nearStart: Float by float("NearStart", 0F, 0F..maxDistance).onChange {
        minOf(it, nearEnd)
    }.markDirtyOnChanged()
    val nearEnd: Float by float("NearEnd", 0F, 0F..maxDistance).onChange {
        it.coerceIn(nearStart, farStart)
    }.markDirtyOnChanged()
    val farStart: Float by float("FarStart", defaultFarStart, 0F..maxDistance).onChange {
        it.coerceIn(nearEnd, farEnd)
    }.markDirtyOnChanged()
    val farEnd: Float by float("FarEnd", defaultFarEnd, 0F..maxDistance).onChange {
        maxOf(farStart, it)
    }.markDirtyOnChanged()

    fun alphaAtDistance(distance: Float): Float =
        distanceFadeFactor(distance, nearStart, nearEnd, farStart, farEnd)

    private val ubo = ClientUniformDefine.DISTANCE_FADE.createSingleBuffer()

    private var uboDirty = true
    private fun <T : Any> Value<T>.markDirtyOnChanged() = onChanged { uboDirty = true }

    fun updateIfDirty() {
        if (uboDirty) {
            ubo.writeStd140 {
                putVec4(nearStart, nearEnd, farStart, farEnd)
            }
            uboDirty = false
        }
    }

    fun bindUniform(pass: RenderPass) {
        pass.setUniform(ClientUniformDefine.DISTANCE_FADE.uboName, ubo)
    }

}

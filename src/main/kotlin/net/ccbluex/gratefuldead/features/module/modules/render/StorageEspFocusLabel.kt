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

package net.ccbluex.gratefuldead.features.module.modules.render

import net.ccbluex.gratefuldead.render.drawRoundedRect
import net.ccbluex.gratefuldead.render.engine.type.BoundingBox2f
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.render.gui.indicator.IndicatorRenderState
import net.ccbluex.gratefuldead.render.gui.indicator.IndicatorVisibility
import net.ccbluex.gratefuldead.render.gui.indicator.NoneIndicatorStyle
import net.ccbluex.gratefuldead.render.esp.EspVisualStyle
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3

/** Only the focused container gets a label, placed at the familiar above-target anchor. */
internal class StorageEspFocusLabel(
    override val pos: Vec3,
    override val indicatorColor: Color4b,
    label: String,
) : IndicatorRenderState {
    private val text = Component.literal(label).visualOrderText
    override val indicatorStyle = NoneIndicatorStyle
    override val visibleAt = listOf(IndicatorVisibility.IN_VIEW)
    override val priority = 100

    override fun measureTag(): BoundingBox2f {
        val font = EspVisualStyle.font
        val halfWidth = font.width(text) / 2F
        return BoundingBox2f(-halfWidth - 4, -3F, halfWidth + 4, font.lineHeight + 3F)
    }

    override fun extractTag(ctx: GuiGraphicsExtractor) {
        val font = EspVisualStyle.font
        val halfWidth = font.width(text) / 2
        ctx.drawRoundedRect((-halfWidth - 4).toFloat(), -3F, (halfWidth + 4).toFloat(), font.lineHeight + 3F,
            radius = EspVisualStyle.GUI_RADIUS, fillColor = Color4b(12, 15, 20, indicatorColor.a * 3 / 4))
        ctx.text(font, text, -halfWidth, 0, indicatorColor.argb, true)
    }
}

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

package net.ccbluex.gratefuldead.integration.screen.impl

import net.ccbluex.gratefuldead.additions.setPosition
import net.ccbluex.gratefuldead.features.module.modules.render.ModuleHud
import net.ccbluex.gratefuldead.integration.screen.CustomScreenType
import net.ccbluex.gratefuldead.integration.screen.ScreenManager
import net.ccbluex.gratefuldead.integration.theme.ThemeManager
import net.ccbluex.gratefuldead.utils.text.asPlainText
import net.ccbluex.gratefuldead.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen

class CustomStandaloneMinecraftScreen(
    val screenType: CustomScreenType
) : Screen("VS-${screenType.routeName.uppercase()}".asPlainText()), AutoCloseable {

    val browser = ThemeManager.openInputAwareImmediate(
        screenType,
        true,
        priority = 20,
        settings = ScreenManager.browserSettings
    ) {
        mc.gui.screen() == this@CustomStandaloneMinecraftScreen
    }

    init {
        browser.visible = false
    }

    private var mouseX = 0.0
    private var mouseY = 0.0

    override fun init() {
        browser.visible = true
        mc.mouseHandler.setPosition(mouseX, mouseY)
    }

    fun sync() {
        browser.reload()
    }

    override fun onClose() {
        browser.visible = false

        mouseX = mc.mouseHandler.xpos()
        mouseY = mc.mouseHandler.ypos()
        mc.mouseHandler.grabMouse()
        super.onClose()
    }

    /**
     * Disable [Screen.extractBlurredBackground]
     */
    override fun isInGameUi(): Boolean {
        return screenType == CustomScreenType.CLICK_GUI && ModuleHud.hudEditorSelected
    }

    override fun extractTransparentBackground(graphics: GuiGraphicsExtractor) {
        // NOOP because we want no background for HUD editor
    }

    override fun isPauseScreen() = false

    override fun close() {
        browser.close()
    }

}

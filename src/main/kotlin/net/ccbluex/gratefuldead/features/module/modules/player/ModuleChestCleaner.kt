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

import net.ccbluex.gratefuldead.event.events.ScheduleInventoryActionEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.utils.collection.Filter
import net.ccbluex.gratefuldead.utils.collection.itemSortedSetOf
import net.ccbluex.gratefuldead.utils.inventory.CheckScreenHandlerTypeValueGroup
import net.ccbluex.gratefuldead.utils.inventory.CheckScreenTitleValueGroup
import net.ccbluex.gratefuldead.utils.inventory.InventoryAction
import net.ccbluex.gratefuldead.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.gratefuldead.utils.inventory.findItemsInContainer
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen

/**
 * ChestCleaner module
 *
 * Automatically drops unwanted items from a chest.
 *
 */
object ModuleChestCleaner : ClientModule(
    "ChestCleaner", ModuleCategories.PLAYER,
    aliases = listOf("ContainerCleaner")
) {
    private val filter by enumChoice("Filter", Filter.WHITELIST)
    private val itemsList by items("Items", itemSortedSetOf())
    private val autoClose by boolean("AutoClose", true)

    private val inventoryConstraints = tree(PlayerInventoryConstraints())
    private val checkScreenHandlerType = tree(CheckScreenHandlerTypeValueGroup(this))
    private val checkScreenTitle = tree(CheckScreenTitleValueGroup(this))

    @Suppress("unused")
    private val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: return@handler
        if (screen is InventoryScreen) return@handler
        if (!checkScreenHandlerType.isValid(screen) || !checkScreenTitle.isValid(screen)) return@handler

        val slots = screen.findItemsInContainer()
        val selectedSlots = slots.filter { !it.itemStack.isEmpty && filter(it.itemStack.item, itemsList) }

        if (selectedSlots.isEmpty()) {
            if (autoClose) {
                event.schedule(inventoryConstraints, InventoryAction.CloseScreen(screen))
            }
        } else {
            val actions = selectedSlots.map { slot -> InventoryAction.Click.performThrow(screen, slot) }
            event.schedule(inventoryConstraints, actions)
        }
    }
}

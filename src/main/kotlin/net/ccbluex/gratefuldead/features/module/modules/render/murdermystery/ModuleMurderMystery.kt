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
package net.ccbluex.gratefuldead.features.module.modules.render.murdermystery

import net.ccbluex.fastutil.forEachIsInstance
import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.events.TagEntityEvent
import net.ccbluex.gratefuldead.event.events.WorldChangeEvent
import net.ccbluex.gratefuldead.event.events.WorldRenderEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.ClientModule
import net.ccbluex.gratefuldead.features.module.ModuleCategories
import net.ccbluex.gratefuldead.render.WorldRenderEnvironment
import net.ccbluex.gratefuldead.render.drawBox
import net.ccbluex.gratefuldead.render.engine.type.Color4b
import net.ccbluex.gratefuldead.render.renderEnvironment
import net.ccbluex.gratefuldead.render.withPositionRelativeToCamera
import net.ccbluex.gratefuldead.utils.entity.interpolateCurrentPosition
import net.ccbluex.gratefuldead.utils.kotlin.Priority
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB

object ModuleMurderMystery : ClientModule("MurderMystery", ModuleCategories.RENDER) {
    var playHurt = false
    var playBow = false

    private val setTeamPrefix by boolean("SetTeamPrefix", true)

    val modes =
        choices(
            "Mode",
            MurderMysteryClassicMode,
            arrayOf(MurderMysteryClassicMode, MurderMysteryInfectionMode, MurderMysteryAssassinationMode),
        )

    init {
        modes.onChanged {
            resetModeState()
        }
    }

    private val currentMode: MurderMysteryMode
        get() = this.modes.activeMode

    override fun onDisabled() {
        this.reset()
    }

    private fun reset() {
        playHurt = false
        playBow = false
        resetModeState()
    }

    private fun resetModeState() {
        this.modes.modes.forEach(MurderMysteryMode::reset)
    }

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> { event ->
        if (playHurt) {
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_HURT, 1F))

            playHurt = false
        }

        if (playBow) {
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.CROSSBOW_SHOOT, 1F))

            playBow = false
        }

        event.renderEnvironment {
            world.entitiesForRendering().forEachIsInstance<ArmorStand> {
                if (it.getItemBySlot(EquipmentSlot.MAINHAND).item is BowItem && it.isInvisible) {
                    renderDroppedBowBox(event.partialTicks, it)
                }
            }
        }
    }

    val packetHandler = handler<PacketEvent> { packetEvent ->
        val world = mc.level ?: return@handler

        when (val packet = packetEvent.packet) {
            is ClientboundSetEquipmentPacket -> {
                val entity = world.getEntity(packet.entity)

                packet.slots
                    .filter {
                        !it.second.isEmpty && it.first.type == EquipmentSlot.Type.HAND
                    }
                    .forEach {
                        handleItem(it.second, entity)
                    }
            }

            is ClientboundLoginPacket, is ClientboundRespawnPacket -> {
                this.reset()
            }
        }
    }

    val tagHandler = handler<TagEntityEvent> {
        if (it.entity !is AbstractClientPlayer) {
            return@handler
        }

        if (!shouldAttack(it.entity)) {
            it.dontTarget()
        }

        val playerType = this.currentMode.getPlayerType(it.entity)
        val entity = it.entity

        val col = when (playerType) {
            MurderMysteryMode.PlayerType.DETECTIVE_LIKE -> Color4b(0, 144, 255)
            MurderMysteryMode.PlayerType.MURDERER -> Color4b(203, 9, 9)
            MurderMysteryMode.PlayerType.NEUTRAL -> return@handler
        }
        if (setTeamPrefix) entity.team?.setPlayerPrefix(playerType.prefix)

        it.color(col, Priority.IMPORTANT_FOR_USAGE_3)
    }

    val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

    private fun handleItem(
        itemStack: ItemStack,
        entity: Entity?,
    ) {
        if (entity !is AbstractClientPlayer) {
            return
        }

        val isSword = MurderMysterySwordDetection.isSword(itemStack)
        val isBow = itemStack.item is BowItem

        when {
            isSword -> currentMode.handleHasSword(entity)
            isBow -> currentMode.handleHasBow(entity)
        }
    }

    private fun WorldRenderEnvironment.renderDroppedBowBox(partialTicks: Float, armorStandEntity: ArmorStand) {
        val box = AABB(-0.6, 0.0, -0.6, 0.6, 2.5, 0.6)
        val pos = armorStandEntity.interpolateCurrentPosition(partialTicks)

        withPositionRelativeToCamera(pos) {
            drawBox(
                box,
                Color4b(127, 255, 212, 100), Color4b(0, 255, 255)
            )
        }
    }

    private fun shouldAttack(entityPlayer: AbstractClientPlayer): Boolean {
        return this.currentMode.shouldAttack(entityPlayer)
    }

    fun disallowsArrowDodge(): Boolean {
        if (!running) {
            return false
        }

        return this.currentMode.disallowsArrowDodge()
    }
}

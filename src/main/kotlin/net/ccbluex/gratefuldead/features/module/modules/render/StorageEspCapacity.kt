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

import net.ccbluex.gratefuldead.config.types.group.ValueGroup

/** High-end defaults with explicit limits for transient presentation data. */
internal object StorageEspCapacity : ValueGroup("Capacity") {
    val retainedBlocks by int("RetainedBlocks", 32768, 4096..131072)
    val retentionMinutes by int("RetentionMinutes", 30, 1..120, "min")
    val animatedEntities by int("AnimatedEntities", 1024, 128..4096)
    val trailPoints by int("TrailPoints", 64, 16..160)
    val pulseBlocks by int("PulseBlocks", 2048, 128..8192)
    val topologyNodes by int("TopologyNodes", 16384, 1024..65536)
    val clusterBlocks by int("ClusterBlocks", 512, 32..4096)
    val clusterCandidates by int("ClusterCandidates", 8192, 512..32768)
    val clusterRange by int("ClusterRange", 96, 16..256, "m")
}

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
package net.ccbluex.gratefuldead.utils.block

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.gratefuldead.event.EventListener
import net.ccbluex.gratefuldead.event.events.ChunkLoadEvent
import net.ccbluex.gratefuldead.event.events.PacketEvent
import net.ccbluex.gratefuldead.event.events.WorldChangeEvent
import net.ccbluex.gratefuldead.event.handler
import net.ccbluex.gratefuldead.features.module.MinecraftShortcuts
import net.ccbluex.gratefuldead.utils.client.clientLogger
import net.ccbluex.gratefuldead.utils.kotlin.CommonCoroutineExceptionHandler
import net.ccbluex.gratefuldead.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.gratefuldead.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.gratefuldead.utils.kotlin.joinAll
import net.ccbluex.gratefuldead.utils.world.forEachSectionBlock
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import java.util.function.BiConsumer
import java.util.function.Predicate
import kotlin.time.measureTime

object ChunkScanner : EventListener, MinecraftShortcuts {

    private val logger = clientLogger("ChunkScanner")

    private val loadedChunks = LongOpenHashSet()

    private val threadLocalBlockPos = ThreadLocal.withInitial(BlockPos::MutableBlockPos)

    private val subscribers = CopyOnWriteArrayList<BlockChangeSubscriber>()
    // World/subscriber teardown waits for active callbacks, then clears their results.
    private val lifecycle = ReentrantReadWriteLock(true)

    fun subscribe(newSubscriber: BlockChangeSubscriber) {
        if (!this.subscribers.addIfAbsent(newSubscriber)) {
            error("Subscriber ${newSubscriber.debugName} already registered")
        }

        val world = mc.level ?: return
        if (this.loadedChunks.isEmpty()) return

        val chunkArray = this.loadedChunks.mapToArray { longChunkPos ->
            world.getChunk(
                ChunkPos.getX(longChunkPos),
                ChunkPos.getZ(longChunkPos)
            )
        }
        val chunks = ObjectArrayList.wrap(chunkArray)
        chunks.removeIf(Predicate(LevelChunk::isEmpty))
        if (chunks.isEmpty) return

        UpdateRequest.NewSubscriber(newSubscriber, chunks)
            .runAsync()
    }

    fun unsubscribe(oldSubscriber: BlockChangeSubscriber) {
        lifecycle.write {
            subscribers.remove(oldSubscriber)
            oldSubscriber.clearAllChunks()
        }
    }

    @Suppress("unused")
    private val chunkLoadHandler = handler<ChunkLoadEvent>(READ_FINAL_STATE) { event ->
        val chunk = world.getChunk(event.x, event.z).takeUnless { it.isEmpty } ?: return@handler

        loadedChunks.add(ChunkPos.pack(event.x, event.z))

        if (subscribers.isEmpty()) return@handler

        UpdateRequest.ChunkLoad(chunk).runAsync()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(READ_FINAL_STATE) { event ->
        if (event.isCancelled) return@handler

        when (val packet = event.packet) {
            is ClientboundBlockUpdatePacket -> {
                if (subscribers.isEmpty()) return@handler

                UpdateRequest.BlockUpdate(packet.pos, packet.blockState).runAsync()
            }

            // All updates are in one section
            is ClientboundSectionBlocksUpdatePacket -> {
                if (subscribers.isEmpty()) return@handler

                UpdateRequest.ChunkSectionUpdate(packet).runAsync()
            }

            is ClientboundForgetLevelChunkPacket -> mc.execute {
                loadedChunks.remove(packet.pos.pack())

                if (subscribers.isEmpty()) return@execute

                UpdateRequest.ChunkUnload(packet.pos).runAsync()
            }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent>(FIRST_PRIORITY) {
        cancelCurrentJobs()
        loadedChunks.clear()
        lifecycle.write {
            subscribers.forEach(BlockChangeSubscriber::clearAllChunks)
        }
    }

    /**
     * When the first request comes in, the dispatcher and the scope will be initialized,
     * and its parallelism cannot be modified
     */
    private val dispatcher = Dispatchers.Default
        .limitedParallelism((Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(2))

    /**
     * The parent job for the current client world.
     * All children will be cancelled on [WorldChangeEvent].
     */
    private val worldJob = SupervisorJob()

    private val scope = CoroutineScope(dispatcher + worldJob + CommonCoroutineExceptionHandler { logger })

    private val queue = ChunkScanQueue(scope)

    /**
     * Cancel all existing enqueue(emit) jobs and scanner jobs
     */
    fun cancelCurrentJobs() {
        worldJob.cancelChildren()
        queue.clear()
    }

    fun stopThread() {
        worldJob.cancel()
        logger.info("Stopped Chunk Scanner Thread!")
    }

    /**
     * @see LevelChunk.getBlockState
     * @see net.minecraft.world.level.chunk.LevelChunkSection.hasOnlyAir
     */
    private suspend fun scanChunkSections(
        chunk: LevelChunk,
        action: BiConsumer<BlockPos, BlockState>
    ) = coroutineScope {
        chunk.sections.forEachIndexed { sectionIndex, section ->
            if (!section.hasOnlyAir()) {
                launch {
                    lifecycle.read {
                        ensureActive()
                        val mutable = threadLocalBlockPos.get()
                        chunk.forEachSectionBlock(sectionIndex, mutable) { pos, state ->
                            ensureActive()
                            action.accept(pos, state)
                        }
                    }
                }
            }
        }
    }

    sealed interface UpdateRequest : suspend (CoroutineScope) -> Unit {
        fun runAsync() {
            when (this) {
                is NewSubscriber -> chunks.forEach { chunk ->
                    queue.enqueue(chunk.pos.pack()) { NewSubscriber(subscriber, listOf(chunk)).invoke(this) }
                }
                is ChunkLoad -> queue.enqueue(chunk.pos.pack(), this)
                is ChunkUnload -> queue.enqueue(pos.pack(), this)
                is BlockUpdate -> queue.enqueue(ChunkPos.pack(blockPos.x shr 4, blockPos.z shr 4), this)
                is ChunkSectionUpdate -> {
                    // The packet exposes updates but no section accessor. Reading a key allocates no positions.
                    var key: Long? = null
                    packet.runUpdates { pos, _ ->
                        if (key == null) key = ChunkPos.pack(pos.x shr 4, pos.z shr 4)
                    }
                    key?.let { queue.enqueue(it, this) }
                }
            }
        }

        /**
         * Scans loaded chunks for new subscriber
         *
         * @param chunks should be non-empty
         */
        class NewSubscriber(val subscriber: BlockChangeSubscriber, val chunks: List<LevelChunk>) : UpdateRequest {
            override suspend fun invoke(scope: CoroutineScope) {
                val duration = measureTime {
                    chunks.forEach {
                        lifecycle.read {
                            scope.ensureActive()
                            if (subscriber in subscribers) subscriber.chunkUpdate(it)
                        }
                    }
                    if (subscriber.shouldCallRecordBlockOnChunkUpdate) {
                        chunks.forEach {
                            scanChunkSections(it) { pos, state ->
                                if (subscriber in subscribers) subscriber.recordBlock(pos, state, cleared = true)
                            }
                        }
                    }
                }

                logger.debug(
                    "Scanning ${chunks.size} chunks for ${subscriber.debugName} took ${duration.inWholeMicroseconds}us"
                )
            }
        }

        /**
         * Scans single new chunk or replaced chunk.
         *
         * @see net.minecraft.client.multiplayer.ClientChunkCache.replaceWithPacketData
         *
         * @param chunk should be non-empty
         */
        class ChunkLoad(val chunk: LevelChunk) : UpdateRequest {
            override suspend fun invoke(scope: CoroutineScope) {
                val duration = measureTime {
                    subscribers.mapToArray {
                        scope.launch {
                            lifecycle.read {
                                ensureActive()
                                if (it in subscribers) {
                                    it.clearChunk(chunk.pos)
                                    it.chunkUpdate(chunk)
                                }
                            }
                        }
                    }.joinAll()

                    // Contains all subscriber that want recordBlock called on a chunk update
                    val subscribersForRecordBlock = subscribers.filter {
                        it.shouldCallRecordBlockOnChunkUpdate
                    }

                    if (subscribersForRecordBlock.isEmpty()) {
                        return@measureTime
                    }

                    scanChunkSections(chunk) { pos, state ->
                        subscribersForRecordBlock.forEach {
                            if (it in subscribers) it.recordBlock(pos, state, cleared = true)
                        }
                    }
                }

                logger.debug(
                    "Scanning chunk (${chunk.pos.x}, ${chunk.pos.z}) took ${duration.inWholeMicroseconds}us"
                )
            }
        }

        class ChunkSectionUpdate(val packet: ClientboundSectionBlocksUpdatePacket) : UpdateRequest {
            override suspend fun invoke(scope: CoroutineScope) {
                lifecycle.read {
                    packet.runUpdates { blockPos, state ->
                        scope.ensureActive()
                        subscribers.forEach { it.recordBlock(blockPos, state, cleared = false) }
                    }
                }
            }
        }

        class ChunkUnload(val pos: ChunkPos) : UpdateRequest {
            override suspend fun invoke(scope: CoroutineScope) {
                lifecycle.read {
                    scope.ensureActive()
                    subscribers.forEach { it.clearChunk(pos) }
                }
            }
        }

        class BlockUpdate(val blockPos: BlockPos, val newState: BlockState) : UpdateRequest {
            override suspend fun invoke(scope: CoroutineScope) {
                lifecycle.read {
                    scope.ensureActive()
                    subscribers.forEach { it.recordBlock(blockPos, newState, cleared = false) }
                }
            }
        }
    }

    interface BlockChangeSubscriber {
        val debugName: String get() = javaClass.simpleName

        /**
         * If this is true [recordBlock] is called on chunk updates and on single block updates.
         * This might be inefficient for some modules, so they can choose to not call that method on chunk updates.
         */
        val shouldCallRecordBlockOnChunkUpdate: Boolean
            get() = true

        /**
         * Registers a block update and asks the subscriber to make a decision about what should be done.
         * This method must be **thread-safe**.
         *
         * @param pos Might be [BlockPos.MutableBlockPos]. Use copy if it needs to be saved.
         * @param state The new [BlockState] of [pos].
         * @param cleared If the block is in section already cleared. Or, does it not need to check existing records
         */
        fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean)

        /**
         * Is called when a chunk is initially loaded or entirely updated.
         *
         * @param chunk a non-empty chunk
         */
        fun chunkUpdate(chunk: LevelChunk)

        fun clearChunk(pos: ChunkPos)

        fun clearAllChunks()
    }

}

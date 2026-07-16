package com.example.hackernews.data.classics

import com.example.hackernews.data.config.ClassicItem
import com.example.hackernews.data.remote.articleIdFor
import java.security.MessageDigest

/**
 * Pure, deterministic selection logic for the offline classics pool.
 *
 * On first launch a random seed is generated and persisted; that seed (plus
 * the round counter) drives a stable SHA-256-keyed shuffle of the pool, so
 * the same persisted state always replays the same batch across restarts.
 * Each refresh advances the cursor by [batchSize]; when the round is
 * exhausted the pool is reshuffled into the next round, avoiding an
 * immediate repeat of the previous batch.
 *
 * No Android dependencies -- fully unit-testable.
 */
class ClassicsSelector(
    pool: List<ClassicItem>,
    val poolVersion: Int,
    private val batchSize: Int = 8,
) {
    private val ids: List<String> = pool.map { articleIdFor(it.url) }
    private val itemsById: Map<String, ClassicItem> = pool.associateBy { articleIdFor(it.url) }

    val poolSize: Int get() = ids.size
    val totalBatches: Int
        get() = if (ids.isEmpty()) 0 else (ids.size + batchSize - 1) / batchSize

    fun initialState(seed: Long): ClassicsState {
        val order = orderFor(seed, 0)
        val end = minOf(batchSize, ids.size)
        return ClassicsState(
            poolVersion = poolVersion,
            seed = seed,
            round = 0,
            cursor = 0,
            batchIds = order.subList(0, end).toList(),
        )
    }

    fun batchFor(state: ClassicsState): List<ClassicItem> {
        val order = orderFor(state.seed, state.round)
        val end = minOf(state.cursor + batchSize, ids.size)
        return order.subList(state.cursor, end).mapNotNull { itemsById[it] }
    }

    fun metaFor(state: ClassicsState): ClassicsMeta = ClassicsMeta(
        round = state.round,
        batchIndex = if (ids.isEmpty()) 0 else state.cursor / batchSize,
        totalBatches = totalBatches,
    )

    fun refresh(state: ClassicsState): ClassicsState {
        val nextStart = state.cursor + batchSize
        return if (nextStart < ids.size) {
            // same round, next batch (the last one may be partial)
            val order = orderFor(state.seed, state.round)
            val end = minOf(nextStart + batchSize, ids.size)
            state.copy(cursor = nextStart, batchIds = order.subList(nextStart, end).toList())
        } else {
            // round exhausted -> reshuffle into the next round
            val newRound = state.round + 1
            val order = orderFor(state.seed, newRound)
            val start = effectiveStart(order, state.batchIds)
            val end = minOf(start + batchSize, ids.size)
            state.copy(round = newRound, cursor = start, batchIds = order.subList(start, end).toList())
        }
    }

    /**
     * Start index for a fresh round's first batch. If the new round's first
     * batch would exactly repeat the previous round's last batch (and the pool
     * spans more than one batch), skip it by starting at [batchSize]; the
     * skipped items reappear in a later round. Otherwise start at 0.
     */
    internal fun effectiveStart(order: List<String>, prevBatchIds: List<String>): Int {
        if (ids.size <= batchSize) return 0
        val firstBatch = order.subList(0, minOf(batchSize, ids.size)).toSet()
        return if (firstBatch == prevBatchIds.toSet()) batchSize else 0
    }

    private fun orderFor(seed: Long, round: Int): List<String> =
        ids.sortedWith(compareBy({ hashKey(seed, round, it) }, { it }))

    private fun hashKey(seed: Long, round: Int, id: String): Long {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$seed|$round|$id".toByteArray(Charsets.UTF_8))
        var value = 0L
        for (i in 0 until 8) value = (value shl 8) or (digest[i].toLong() and 0xFF)
        return value
    }
}

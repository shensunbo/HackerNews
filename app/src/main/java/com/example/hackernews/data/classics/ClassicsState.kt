package com.example.hackernews.data.classics

/**
 * Persisted cursor into the offline classics pool.
 *
 * The batch shown to the user is fully determined by [seed], [round] and
 * [cursor]; [batchIds] is a denormalized cache of the currently displayed
 * items (and the reference used to avoid repeating the last batch when a
 * round wraps). [poolVersion] tracks which shipped pool this state belongs
 * to -- a mismatch forces re-initialization.
 */
data class ClassicsState(
    val poolVersion: Int,
    val seed: Long,
    val round: Int,
    val cursor: Int,
    val batchIds: List<String>,
)

/** Progress metadata derived from a [ClassicsState] for display. */
data class ClassicsMeta(
    val round: Int,
    val batchIndex: Int, // 0-based within the current round
    val totalBatches: Int,
)

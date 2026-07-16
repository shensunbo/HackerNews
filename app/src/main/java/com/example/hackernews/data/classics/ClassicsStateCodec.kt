package com.example.hackernews.data.classics

/**
 * Pure (Android-free) encoding for the persisted [ClassicsState].
 *
 * The DataStore layer stores each field under a primitive preference key;
 * this object owns the key names and the batch-id list <-> CSV conversion so
 * the serialization can be unit-tested without a device.
 */
object ClassicsStateCodec {
    const val KEY_POOL_VERSION = "classics_pool_version"
    const val KEY_SEED = "classics_seed"
    const val KEY_ROUND = "classics_round"
    const val KEY_CURSOR = "classics_cursor"
    const val KEY_BATCH_IDS = "classics_batch_ids"

    fun decode(
        poolVersion: Int?,
        seed: Long?,
        round: Int?,
        cursor: Int?,
        batchIds: String?,
    ): ClassicsState? {
        val s = seed ?: return null // no seed == never initialized
        return ClassicsState(
            poolVersion = poolVersion ?: 1,
            seed = s,
            round = round ?: 0,
            cursor = cursor ?: 0,
            batchIds = decodeBatchIds(batchIds),
        )
    }

    fun encodeBatchIds(ids: List<String>): String = ids.joinToString(",")

    fun decodeBatchIds(raw: String?): List<String> =
        raw?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
}

package com.example.hackernews.data.classics

import com.example.hackernews.data.config.ClassicItem
import com.example.hackernews.data.remote.articleIdFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicsSelectorTest {
    private val seed = 42L

    private fun item(i: Int) = ClassicItem("t$i", "https://x.io/$i", "s$i", "backend")
    private fun id(i: Int) = articleIdFor("https://x.io/$i")
    private fun pool(n: Int) = (0 until n).map(::item)
    private fun selector(n: Int, batchSize: Int = 8) =
        ClassicsSelector(pool(n), poolVersion = 2, batchSize = batchSize)

    private fun idsOf(items: List<ClassicItem>) = items.map { articleIdFor(it.url) }

    @Test fun initialState_returnsFirstBatchAndZeroCursor() {
        val s = selector(24)
        val state = s.initialState(seed)

        assertEquals(0, state.round)
        assertEquals(0, state.cursor)
        assertEquals(8, state.batchIds.size)
        assertEquals(state.batchIds, idsOf(s.batchFor(state)))
    }

    @Test fun refresh_advancesToNextBatchSameRound() {
        val s = selector(24)
        val first = s.initialState(seed)

        val next = s.refresh(first)

        assertEquals(0, next.round)
        assertEquals(8, next.cursor)
        assertEquals(8, next.batchIds.size)
        assertFalse(first.batchIds.any { it in next.batchIds })
    }

    @Test fun sameStateProducesSameOrderAcrossSeeds() {
        val s = selector(24)
        val state = s.initialState(seed)

        // determinism: identical state always yields the identical batch
        assertEquals(s.batchFor(state), s.batchFor(state))

        // different seeds produce distinct first batches (at least 2 of 10 differ)
        val distinct = (1..10).map { s.initialState(it.toLong()).batchIds }.toSet()
        assertTrue("expected seeds to yield >1 distinct batch, got ${distinct.size}", distinct.size > 1)
    }

    @Test fun firstRoundShowsAllItemsExactlyOnce() {
        val n = 24
        val s = selector(n)
        var state = s.initialState(seed)
        val seen = state.batchIds.toMutableList()

        // consume round 0 until the next refresh would wrap to round 1
        while (true) {
            val next = s.refresh(state)
            if (next.round != state.round) break
            state = next
            seen += state.batchIds
        }

        assertEquals(n, seen.size)
        assertEquals(n, seen.toSet().size)
        assertEquals((0 until n).map(::id).toSet(), seen.toSet())
    }

    @Test fun partialLastBatchHandled() {
        val s = selector(10) // batches: 8 then 2

        var state = s.initialState(seed)
        assertEquals(8, state.batchIds.size)

        state = s.refresh(state) // cursor 8 -> [8,10) = 2 items
        assertEquals(0, state.round)
        assertEquals(8, state.cursor)
        assertEquals(2, state.batchIds.size)
    }

    @Test fun roundWrapReshufflesIntoNewRound() {
        val s = selector(8) // exactly one batch per round

        val state = s.refresh(s.initialState(seed))

        assertEquals(1, state.round)
        assertEquals(0, state.cursor)
        // pool == batch: cannot avoid showing all items each round
        assertEquals(s.initialState(seed).batchIds.toSet(), state.batchIds.toSet())
    }

    @Test fun roundTransitionDoesNotImmediatelyRepeatLastBatch() {
        val s = selector(24)
        var state = s.initialState(seed)

        // advance to the last batch of round 0: [16,24)
        repeat(2) { state = s.refresh(state) }
        assertEquals(0, state.round)
        assertEquals(16, state.cursor)
        val lastBatch = state.batchIds

        // wrap into round 1; first batch must differ from round 0's last batch
        state = s.refresh(state)
        assertEquals(1, state.round)
        assertNotEquals(lastBatch.toSet(), state.batchIds.toSet())
    }

    @Test fun restartReplaysSameBatchFromPersistedState() {
        val s = selector(24)
        val state = s.initialState(seed).let { s.refresh(it) }.let { s.refresh(it) } // cursor 16

        // a freshly-constructed selector with the same pool replays the same batch
        val replayed = selector(24).batchFor(state)
        assertEquals(state.batchIds, idsOf(replayed))
    }

    @Test fun metaForReportsBatchIndexAndTotal() {
        val s = selector(24)
        val state = s.refresh(s.initialState(seed)) // cursor 8 -> batch 2 of 3

        val meta = s.metaFor(state)
        assertEquals(0, meta.round)
        assertEquals(1, meta.batchIndex)
        assertEquals(3, meta.totalBatches)
    }

    @Test fun effectiveStart_skipsWhenFirstBatchMatchesPrevious() {
        val s = selector(24)
        val order = (0 until 24).map(::id)
        val prevBatch = order.subList(0, 8) // equals the new round's first batch -> collision

        assertEquals(8, s.effectiveStart(order, prevBatch))
    }

    @Test fun effectiveStart_zeroWhenNoCollision() {
        val s = selector(24)
        val order = (0 until 24).map(::id)
        val prevBatch = order.subList(8, 16) // differs from first 8

        assertEquals(0, s.effectiveStart(order, prevBatch))
    }

    @Test fun effectiveStart_zeroWhenPoolNotLargerThanBatch() {
        val s = selector(8)
        val order = (0 until 8).map(::id)
        val prevBatch = order.subList(0, 8) // collides but pool == batch

        assertEquals(0, s.effectiveStart(order, prevBatch))
    }
}

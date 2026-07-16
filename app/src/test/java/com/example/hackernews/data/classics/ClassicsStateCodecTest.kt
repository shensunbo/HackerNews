package com.example.hackernews.data.classics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClassicsStateCodecTest {

    @Test fun decode_allFieldsNull_returnsNullUninitializedState() {
        assertNull(ClassicsStateCodec.decode(null, null, null, null, null))
    }

    @Test fun decode_seedPresent_appliesDefaultsForOtherFields() {
        val state = ClassicsStateCodec.decode(
            poolVersion = null,
            seed = 42L,
            round = null,
            cursor = null,
            batchIds = null,
        )
        assertEquals(42L, state!!.seed)
        assertEquals(1, state.poolVersion)
        assertEquals(0, state.round)
        assertEquals(0, state.cursor)
        assertEquals(emptyList<String>(), state.batchIds)
    }

    @Test fun decode_parsesCommaSeparatedBatchIds() {
        val state = ClassicsStateCodec.decode(2, 7L, 3, 16, "a,b,c")
        assertEquals(2, state!!.poolVersion)
        assertEquals(7L, state.seed)
        assertEquals(3, state.round)
        assertEquals(16, state.cursor)
        assertEquals(listOf("a", "b", "c"), state.batchIds)
    }

    @Test fun batchIds_roundTripThroughCodec() {
        val ids = listOf("abc123", "def456", "ghi789")
        val encoded = ClassicsStateCodec.encodeBatchIds(ids)
        assertEquals(ids, ClassicsStateCodec.decodeBatchIds(encoded))
    }

    @Test fun decodeBatchIds_handlesBlankAndEmpty() {
        assertEquals(emptyList<String>(), ClassicsStateCodec.decodeBatchIds(""))
        assertEquals(emptyList<String>(), ClassicsStateCodec.decodeBatchIds("  "))
        assertEquals(listOf("x", "y"), ClassicsStateCodec.decodeBatchIds("x,,y,"))
    }

    @Test fun fullState_roundTripThroughEncodeDecode() {
        val original = ClassicsState(
            poolVersion = 2,
            seed = 99L,
            round = 5,
            cursor = 32,
            batchIds = listOf("id1", "id2", "id3", "id4"),
        )
        val decoded = ClassicsStateCodec.decode(
            poolVersion = original.poolVersion,
            seed = original.seed,
            round = original.round,
            cursor = original.cursor,
            batchIds = ClassicsStateCodec.encodeBatchIds(original.batchIds),
        )
        assertEquals(original, decoded)
    }
}

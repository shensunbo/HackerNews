package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {
    private val backend = Topic("backend", "B", true, 1f, emptyList(), listOf("database", "kubernetes"))
    private val ai = Topic("ai", "A", true, 1f, emptyList(), listOf("LLM", "rag"))

    @Test fun matches_caseInsensitiveSubstring() {
        assertEquals(listOf("backend"), matchTopics("Scaling our DataBase layer", listOf(backend, ai)))
    }
    @Test fun matches_multipleTopics() {
        val r = matchTopics("Kubernetes for LLM serving", listOf(backend, ai))
        assertTrue(r.containsAll(listOf("backend", "ai")))
    }
    @Test fun matches_noneReturnsEmpty() {
        assertTrue(matchTopics("A poem about spring", listOf(backend, ai)).isEmpty())
    }
}

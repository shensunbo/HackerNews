package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedRankerTest {
    private val now = 1_000_000_000_000L
    private fun art(id: String, topic: String, ageHours: Long, score: Int? = null) =
        Article(id, id, "https://e/$id", "", "src", listOf(topic),
            now - ageHours * 3_600_000L, false, score)

    @Test fun higherWeightRanksFirst_sameAge() {
        val a = art("a", "backend", 1); val b = art("b", "frontend", 1)
        val out = rankFeed(listOf(b, a), mapOf("backend" to 2.0f, "frontend" to 1.0f), now)
        assertEquals("a", out.first().id)
    }
    @Test fun newerRanksFirst_sameWeight() {
        val old = art("old", "backend", 100); val fresh = art("fresh", "backend", 1)
        val out = rankFeed(listOf(old, fresh), mapOf("backend" to 1.0f), now)
        assertEquals("fresh", out.first().id)
    }
    @Test fun hnScoreBreaksTie() {
        val hot = art("hot", "backend", 1, score = 300); val cold = art("cold", "backend", 1, score = 0)
        val out = rankFeed(listOf(cold, hot), mapOf("backend" to 1.0f), now)
        assertEquals("hot", out.first().id)
    }
}

package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleMergeTest {
    private fun art(topics: List<String>, summary: String = "", score: Int? = null, pub: Long = 0) =
        Article("same", "T", "https://e/x", summary, "src", topics, pub, false, score)

    @Test fun mergesSameIdUnioningTopicsAndKeepingSummary() {
        val a = art(listOf("backend"), summary = "", score = 10, pub = 100)
        val b = art(listOf("ai"), summary = "详细简介", score = null, pub = 200)
        val out = mergeArticles(listOf(a, b))
        assertEquals(1, out.size)
        assertEquals(setOf("backend", "ai"), out[0].topicIds.toSet())
        assertEquals("详细简介", out[0].summary)
        assertEquals(200L, out[0].publishedAt)
        assertEquals(10, out[0].score)
    }
    @Test fun keepsDistinctIds() {
        val a = Article("a", "A", "u", "", "s", listOf("x"), 0, false, null)
        val b = Article("b", "B", "u", "", "s", listOf("x"), 0, false, null)
        assertEquals(2, mergeArticles(listOf(a, b)).size)
    }
}

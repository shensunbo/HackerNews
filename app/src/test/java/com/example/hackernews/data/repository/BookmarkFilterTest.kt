package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkFilterTest {
    private val kotlin = Article(
        id = "kotlin",
        title = "Kotlin Coroutines",
        url = "https://example.com/kotlin",
        summary = "",
        source = "Android Blog",
        topicIds = listOf("languages"),
        publishedAt = 1L,
        isBookmarked = true,
    )
    private val database = Article(
        id = "database",
        title = "Database Internals",
        url = "https://example.com/database",
        summary = "",
        source = "Architecture Weekly",
        topicIds = listOf("backend"),
        publishedAt = 1L,
        isBookmarked = true,
    )

    @Test fun topicAndQueryFiltersAreCombined() {
        val result = filterBookmarks(
            articles = listOf(kotlin, database),
            topicId = "languages",
            query = "android",
        )

        assertEquals(listOf("kotlin"), result.map { it.id })
    }

    @Test fun blankFiltersReturnAllBookmarks() {
        assertEquals(2, filterBookmarks(listOf(kotlin, database), null, "").size)
    }
}

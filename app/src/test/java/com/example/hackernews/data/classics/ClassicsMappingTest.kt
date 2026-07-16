package com.example.hackernews.data.classics

import com.example.hackernews.data.config.ClassicItem
import com.example.hackernews.domain.model.ArticleOrigin
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassicsMappingTest {
    @Test fun classicItemMapsToClassicArticle() {
        val item = ClassicItem(
            title = "The Twelve-Factor App",
            url = "https://12factor.net",
            summary = "summary",
            topicId = "backend",
        )

        val article = item.toArticle()

        assertEquals(ArticleOrigin.CLASSIC, article.origin)
        assertEquals(listOf("backend"), article.topicIds)
        assertEquals("Must Read", article.source)
    }

    @Test fun blankTopicIdMapsToEmptyTopicList() {
        val item = ClassicItem(title = "T", url = "https://x.io", summary = "s", topicId = "")

        assertEquals(emptyList<String>(), item.toArticle().topicIds)
    }
}

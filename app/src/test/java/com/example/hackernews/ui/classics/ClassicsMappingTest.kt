package com.example.hackernews.ui.classics

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
    }
}

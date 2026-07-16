package com.example.hackernews.data.classics

import com.example.hackernews.data.config.ClassicItem
import com.example.hackernews.data.remote.articleIdFor
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ArticleOrigin

/** Map a [ClassicItem] from the offline pool to a domain [Article]. */
fun ClassicItem.toArticle(): Article = Article(
    id = articleIdFor(url),
    title = title,
    url = url,
    summary = summary,
    source = "Must Read",
    topicIds = topicId.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty(),
    publishedAt = 0L,
    origin = ArticleOrigin.CLASSIC,
)

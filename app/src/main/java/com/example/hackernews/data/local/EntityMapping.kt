package com.example.hackernews.data.local

import com.example.hackernews.domain.model.Article

fun Article.toEntity() = ArticleEntity(
    id = id, title = title, url = url, summary = summary, source = source,
    topicIds = topicIds.joinToString(","), publishedAt = publishedAt,
    isBookmarked = isBookmarked, score = score, origin = origin,
)

fun ArticleEntity.toArticle() = Article(
    id = id, title = title, url = url, summary = summary, source = source,
    topicIds = topicIds.split(",").filter { it.isNotBlank() },
    publishedAt = publishedAt, isBookmarked = isBookmarked, score = score, origin = origin,
)

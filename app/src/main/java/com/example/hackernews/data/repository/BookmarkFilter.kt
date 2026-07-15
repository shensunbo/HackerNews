package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article

fun filterBookmarks(
    articles: List<Article>,
    topicId: String?,
    query: String,
): List<Article> = articles
    .filter { article -> topicId == null || topicId in article.topicIds }
    .filter { article ->
        query.isBlank() ||
            article.title.contains(query, ignoreCase = true) ||
            article.source.contains(query, ignoreCase = true)
    }

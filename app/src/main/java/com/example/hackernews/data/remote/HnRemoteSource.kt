package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class HnRemoteSource(
    private val api: HnApiService,
    private val limit: Int = 60,
) {
    suspend fun fetch(enabledTopics: List<Topic>): List<Article> = coroutineScope {
        if (enabledTopics.isEmpty()) return@coroutineScope emptyList()
        val ids = runCatching { api.bestStoryIds() }.getOrElse { emptyList() }.take(limit)
        ids.map { id -> async { runCatching { api.item(id) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()
            .filter { it.type == "story" && !it.url.isNullOrBlank() && !it.title.isNullOrBlank() }
            .mapNotNull { item ->
                val topics = matchTopics(item.title!!, enabledTopics)
                if (topics.isEmpty()) null
                else Article(
                    id = articleIdFor(item.url!!),
                    title = item.title,
                    url = item.url,
                    summary = "",
                    source = "Hacker News",
                    topicIds = topics,
                    publishedAt = (item.time ?: 0L) * 1000L,
                    score = item.score,
                )
            }
    }
}

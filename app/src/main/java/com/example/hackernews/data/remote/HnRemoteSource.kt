package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class HnRemoteSource(
    private val api: HnApiService,
    private val limit: Int = 60,
) : RemoteArticleSource {
    override suspend fun fetch(
        topics: List<Topic>,
        nowMillis: Long,
    ): RemoteFetchResult = coroutineScope {
        if (topics.isEmpty()) return@coroutineScope RemoteFetchResult.noRequests()
        val idsResult = runCatching { api.bestStoryIds() }
        if (idsResult.isFailure) return@coroutineScope RemoteFetchResult.failure()

        val itemResults = idsResult.getOrThrow().take(limit)
            .map { id -> async { runCatching { api.item(id) } } }
            .awaitAll()
        val articles = itemResults.mapNotNull { result ->
            val item = result.getOrNull() ?: return@mapNotNull null
            if (item.type != "story") return@mapNotNull null
            val title = item.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val url = item.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val topicIds = matchTopics(title, topics)
            if (topicIds.isEmpty()) return@mapNotNull null
            Article(
                id = articleIdFor(url),
                title = title,
                url = url,
                summary = "",
                source = "Hacker News",
                topicIds = topicIds,
                publishedAt = (item.time ?: 0L) * 1000L,
                score = item.score,
            )
        }
        RemoteFetchResult(
            articles = articles,
            successfulRequests = 1 + itemResults.count { it.isSuccess },
            failedRequests = itemResults.count { it.isFailure },
        )
    }
}

package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic

data class RemoteFetchResult(
    val articles: List<Article>,
    val successfulRequests: Int,
    val failedRequests: Int,
) {
    operator fun plus(other: RemoteFetchResult) = RemoteFetchResult(
        articles = articles + other.articles,
        successfulRequests = successfulRequests + other.successfulRequests,
        failedRequests = failedRequests + other.failedRequests,
    )

    companion object {
        fun success(
            articles: List<Article>,
            successfulRequests: Int = 1,
        ) = RemoteFetchResult(articles, successfulRequests, failedRequests = 0)

        fun failure(failedRequests: Int = 1) =
            RemoteFetchResult(emptyList(), successfulRequests = 0, failedRequests)

        fun noRequests() = RemoteFetchResult(
            articles = emptyList(),
            successfulRequests = 0,
            failedRequests = 0,
        )
    }
}

fun interface RemoteArticleSource {
    suspend fun fetch(topics: List<Topic>, nowMillis: Long): RemoteFetchResult
}

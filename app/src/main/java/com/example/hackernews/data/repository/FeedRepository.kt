package com.example.hackernews.data.repository

import com.example.hackernews.data.config.TopicConfigSource
import com.example.hackernews.data.local.ArticleDao
import com.example.hackernews.data.local.TopicPreferencesSource
import com.example.hackernews.data.local.toArticle
import com.example.hackernews.data.local.toEntity
import com.example.hackernews.data.remote.RemoteArticleSource
import com.example.hackernews.data.remote.RemoteFetchResult
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class RefreshResult(val count: Int, val failed: Boolean)

interface FeedDataSource {
    fun feedStream(): Flow<List<Article>>
    suspend fun refresh(): RefreshResult
    suspend fun toggleBookmark(id: String)
}

class FeedRepository(
    private val dao: ArticleDao,
    private val prefs: TopicPreferencesSource,
    configLoader: TopicConfigSource,
    private val hn: RemoteArticleSource,
    private val rss: RemoteArticleSource,
    private val now: () -> Long = { System.currentTimeMillis() },
) : FeedDataSource {
    private val baseTopics: List<Topic> = configLoader.loadTopics()

    fun topicsStream(): Flow<List<Topic>> = prefs.topicPrefs().map { overrides ->
        baseTopics.map { t ->
            val o = overrides[t.id]
            t.copy(enabled = o?.enabled ?: t.enabled, weight = o?.weight ?: t.weight)
        }
    }

    override fun feedStream(): Flow<List<Article>> =
        combine(dao.feedStream(), topicsStream()) { entities, topics ->
            val enabledIds = topics.filter { it.enabled }.map { it.id }.toSet()
            val weights = topics.associate { it.id to it.weight }
            val articles = entities.map { it.toArticle() }
                .filter { a -> a.topicIds.any(enabledIds::contains) }
            rankFeed(articles, weights, now())
        }

    override suspend fun refresh(): RefreshResult {
        val enabled = topicsStream().first().filter { it.enabled }
        if (enabled.isEmpty()) return RefreshResult(0, failed = false)
        val nowMs = now()
        val (hnR, rssR) = coroutineScope {
            val h = async {
                runCatching { hn.fetch(enabled, nowMs) }
                    .getOrElse { RemoteFetchResult.failure() }
            }
            val r = async {
                runCatching { rss.fetch(enabled, nowMs) }
                    .getOrElse { RemoteFetchResult.failure() }
            }
            h.await() to r.await()
        }
        val remote = hnR + rssR
        val merged = mergeArticles(remote.articles)
        dao.upsertPreservingBookmark(merged.map { it.toEntity() }, firstSeenAt = nowMs)
        return RefreshResult(
            count = merged.size,
            failed = remote.successfulRequests == 0 && remote.failedRequests > 0,
        )
    }

    fun bookmarksStream(topicId: String?, query: String): Flow<List<Article>> =
        dao.bookmarksStream().map { list ->
            list.map { it.toArticle() }
                .filter { topicId == null || it.topicIds.contains(topicId) }
                .filter { q ->
                    query.isBlank() || q.title.contains(query, true) || q.source.contains(query, true)
                }
        }

    override suspend fun toggleBookmark(id: String) {
        val current = dao.getById(id)?.isBookmarked ?: false
        dao.setBookmarked(id, !current)
    }

    fun bookmarkedIdsStream(): Flow<Set<String>> = dao.bookmarksStream().map { entities ->
        entities.map { it.id }.toSet()
    }

    suspend fun toggleBookmarkForArticle(article: Article) {
        val existing = dao.getById(article.id)
        if (existing == null) {
            dao.upsertPreservingBookmark(listOf(article.toEntity()), firstSeenAt = now())
            dao.setBookmarked(article.id, true)
        } else {
            dao.setBookmarked(article.id, !existing.isBookmarked)
        }
    }
}

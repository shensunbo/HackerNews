package com.example.hackernews.data.repository

import com.example.hackernews.data.config.AssetConfigLoader
import com.example.hackernews.data.local.ArticleDao
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.local.toArticle
import com.example.hackernews.data.local.toEntity
import com.example.hackernews.data.remote.HnRemoteSource
import com.example.hackernews.data.remote.RssRemoteSource
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class RefreshResult(val count: Int, val failed: Boolean)

class FeedRepository(
    private val dao: ArticleDao,
    private val prefs: PreferencesStore,
    configLoader: AssetConfigLoader,
    private val hn: HnRemoteSource,
    private val rss: RssRemoteSource,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val baseTopics: List<Topic> = configLoader.loadTopics()

    fun topicsStream(): Flow<List<Topic>> = prefs.topicPrefs().map { overrides ->
        baseTopics.map { t ->
            val o = overrides[t.id]
            t.copy(enabled = o?.enabled ?: t.enabled, weight = o?.weight ?: t.weight)
        }
    }

    fun feedStream(): Flow<List<Article>> =
        combine(dao.feedStream(), topicsStream()) { entities, topics ->
            val enabledIds = topics.filter { it.enabled }.map { it.id }.toSet()
            val weights = topics.associate { it.id to it.weight }
            val articles = entities.map { it.toArticle() }
                .filter { a -> a.topicIds.any(enabledIds::contains) }
            rankFeed(articles, weights, now())
        }

    suspend fun refresh(): RefreshResult {
        val enabled = topicsStream().first().filter { it.enabled }
        if (enabled.isEmpty()) return RefreshResult(0, failed = false)
        val nowMs = now()
        val (hnR, rssR) = coroutineScope {
            val h = async { runCatching { hn.fetch(enabled) } }
            val r = async { runCatching { rss.fetch(enabled, nowMs) } }
            h.await() to r.await()
        }
        val merged = mergeArticles(hnR.getOrElse { emptyList() } + rssR.getOrElse { emptyList() })
        dao.upsertPreservingBookmark(merged.map { it.toEntity() })
        return RefreshResult(merged.size, failed = hnR.isFailure && rssR.isFailure)
    }

    fun bookmarksStream(topicId: String?, query: String): Flow<List<Article>> =
        dao.bookmarksStream().map { list ->
            list.map { it.toArticle() }
                .filter { topicId == null || it.topicIds.contains(topicId) }
                .filter { q ->
                    query.isBlank() || q.title.contains(query, true) || q.source.contains(query, true)
                }
        }

    suspend fun toggleBookmark(id: String) {
        val current = dao.getById(id)?.isBookmarked ?: false
        dao.setBookmarked(id, !current)
    }
}

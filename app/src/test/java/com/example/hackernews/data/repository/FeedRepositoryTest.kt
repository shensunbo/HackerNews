package com.example.hackernews.data.repository

import com.example.hackernews.data.config.TopicConfigSource
import com.example.hackernews.data.local.ArticleDao
import com.example.hackernews.data.local.ArticleEntity
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.local.TopicPreferencesSource
import com.example.hackernews.data.remote.RemoteArticleSource
import com.example.hackernews.data.remote.RemoteFetchResult
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ArticleOrigin
import com.example.hackernews.domain.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedRepositoryTest {
    private val backend = Topic(
        id = "backend",
        name = "Backend",
        enabled = true,
        weight = 1.2f,
        feeds = listOf("https://example.com/feed"),
        keywords = listOf("database"),
    )

    @Test fun allRemoteRequestsFail_marksRefreshFailed() = runTest {
        val dao = FakeArticleDao()
        val repository = repository(
            dao = dao,
            hnResult = RemoteFetchResult.failure(),
            rssResult = RemoteFetchResult.failure(),
        )

        val result = repository.refresh()

        assertTrue(result.failed)
        assertEquals(0, result.count)
        assertTrue(dao.feedStream().first().isEmpty())
    }

    @Test fun partialRemoteFailure_cachesSuccessfulArticles() = runTest {
        val dao = FakeArticleDao()
        val article = article("fresh")
        val repository = repository(
            dao = dao,
            hnResult = RemoteFetchResult.success(listOf(article)),
            rssResult = RemoteFetchResult.failure(),
        )

        val result = repository.refresh()

        assertFalse(result.failed)
        assertEquals(1, result.count)
        assertEquals("fresh", dao.feedStream().first().single().id)
    }

    @Test fun singlePreferenceOverride_preservesOtherAssetDefault() = runTest {
        val preferences = mapOf(
            "backend" to PreferencesStore.TopicPref(enabled = false, weight = null),
        )
        val repository = repository(preferences = preferences)

        val topic = repository.topicsStream().first().single()

        assertFalse(topic.enabled)
        assertEquals(1.2f, topic.weight)
    }

    private fun repository(
        dao: FakeArticleDao = FakeArticleDao(),
        preferences: Map<String, PreferencesStore.TopicPref> = emptyMap(),
        hnResult: RemoteFetchResult = RemoteFetchResult.success(emptyList()),
        rssResult: RemoteFetchResult = RemoteFetchResult.success(emptyList()),
    ) = FeedRepository(
        dao = dao,
        prefs = FakePreferences(preferences),
        configLoader = TopicConfigSource { listOf(backend) },
        hn = FakeRemoteSource(hnResult),
        rss = FakeRemoteSource(rssResult),
        now = { 10_000L },
    )

    private fun article(id: String) = Article(
        id = id,
        title = id,
        url = "https://example.com/$id",
        summary = "summary",
        source = "source",
        topicIds = listOf("backend"),
        publishedAt = 1_000L,
    )

    private class FakePreferences(
        private val preferences: Map<String, PreferencesStore.TopicPref>,
    ) : TopicPreferencesSource {
        override fun topicPrefs(): Flow<Map<String, PreferencesStore.TopicPref>> = flowOf(preferences)
    }

    private class FakeRemoteSource(
        private val result: RemoteFetchResult,
    ) : RemoteArticleSource {
        override suspend fun fetch(topics: List<Topic>, nowMillis: Long): RemoteFetchResult = result
    }

    private class FakeArticleDao : ArticleDao {
        private val entities = MutableStateFlow<List<ArticleEntity>>(emptyList())

        override fun feedStream(): Flow<List<ArticleEntity>> = entities

        override fun bookmarksStream(): Flow<List<ArticleEntity>> =
            MutableStateFlow(entities.value.filter { it.isBookmarked })

        override suspend fun getById(id: String): ArticleEntity? = entities.value.find { it.id == id }

        override suspend fun setBookmarked(id: String, value: Boolean) {
            entities.value = entities.value.map { if (it.id == id) it.copy(isBookmarked = value) else it }
        }

        override suspend fun bookmarkFlag(id: String): Boolean? = getById(id)?.isBookmarked

        override suspend fun insertRaw(
            id: String,
            title: String,
            url: String,
            summary: String,
            source: String,
            topicIds: String,
            publishedAt: Long,
            isBookmarked: Boolean,
            score: Int?,
            origin: ArticleOrigin,
        ) {
            val item = ArticleEntity(
                id, title, url, summary, source, topicIds, publishedAt, isBookmarked, score, origin,
            )
            entities.value = entities.value.filterNot { it.id == id } + item
        }
    }
}

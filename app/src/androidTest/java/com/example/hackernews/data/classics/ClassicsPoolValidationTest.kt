package com.example.hackernews.data.classics

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hackernews.data.config.AssetConfigLoader
import com.example.hackernews.data.remote.articleIdFor
import com.example.hackernews.data.remote.normalizeUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the shipped classics.json. The >=480 target belongs to the one-time
 * collector's self-check (see tools/classics-collector), not the shipped pool;
 * here we only assert the pool is well-formed enough for the runtime.
 */
@RunWith(AndroidJUnit4::class)
class ClassicsPoolValidationTest {
    private val loader = AssetConfigLoader(ApplicationProvider.getApplicationContext())
    private val pool = loader.loadClassicsPool()

    @Test fun poolIsLargeEnoughForAtLeastOneBatch() {
        assertTrue("pool must hold at least one batch (8), got ${pool.items.size}",
            pool.items.size >= 8)
    }

    @Test fun poolVersionIsSet() {
        assertTrue("poolVersion must be >= 1, got ${pool.poolVersion}", pool.poolVersion >= 1)
    }

    @Test fun articleIdsAreUnique() {
        val ids = pool.items.map { articleIdFor(it.url) }
        assertEquals("duplicate article ids (normalized-URL collision)",
            ids.size, ids.toSet().size)
    }

    @Test fun normalizedUrlsAreUnique() {
        val urls = pool.items.map { normalizeUrl(it.url) }
        assertEquals("duplicate normalized URLs", urls.size, urls.toSet().size)
    }

    @Test fun requiredFieldsArePresent() {
        pool.items.forEach { item ->
            assertTrue("blank title for ${item.url}", item.title.isNotBlank())
            assertTrue("blank url", item.url.isNotBlank())
            assertTrue("blank summary for ${item.url}", item.summary.isNotBlank())
            assertTrue("blank topicId for ${item.url}", item.topicId.isNotBlank())
        }
    }

    @Test fun urlsAreHttpOrHttps() {
        pool.items.forEach { item ->
            assertTrue("not http(s): ${item.url}",
                item.url.startsWith("http://") || item.url.startsWith("https://"))
        }
    }

    @Test fun topicIdsReferenceKnownTopics() {
        val knownTopics = loader.loadTopics().map { it.id }.toSet()
        pool.items.forEach { item ->
            assertTrue("unknown topicId ${item.topicId} for ${item.url}",
                item.topicId in knownTopics)
        }
    }
}

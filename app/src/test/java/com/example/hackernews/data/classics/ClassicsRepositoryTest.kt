package com.example.hackernews.data.classics

import com.example.hackernews.data.config.ClassicItem
import com.example.hackernews.data.repository.Bookmarks
import com.example.hackernews.domain.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicsRepositoryTest {
    private val pool = (0 until 16).map {
        ClassicItem("t$it", "https://x.io/$it", "s$it", "backend")
    }
    private val selector = ClassicsSelector(pool, poolVersion = 2, batchSize = 8)

    private fun repo(
        stateStore: FakeStateSource = FakeStateSource(),
        bookmarks: FakeBookmarks = FakeBookmarks(),
        seed: Long = 42L,
    ) = ClassicsRepository(selector, stateStore, bookmarks, seedGenerator = { seed })

    @Test fun ensureInitialized_firstLaunch_generatesSeedAndPersistsRound0() = runTest {
        val store = FakeStateSource()
        val repo = repo(store, seed = 42L)

        repo.ensureInitialized()

        val saved = store.saved
        assertNotNull(saved)
        assertEquals(42L, saved!!.seed)
        assertEquals(2, saved.poolVersion)
        assertEquals(0, saved.round)
        assertEquals(0, saved.cursor)
        assertEquals(8, saved.batchIds.size)
    }

    @Test fun ensureInitialized_poolVersionMismatch_reinitializes() = runTest {
        val store = FakeStateSource(
            initial = ClassicsState(poolVersion = 1, seed = 1L, round = 5, cursor = 8, batchIds = listOf("x")),
        )
        val repo = repo(store, seed = 99L)

        repo.ensureInitialized()

        val saved = store.saved!!
        assertEquals(2, saved.poolVersion) // current selector version
        assertEquals(99L, saved.seed)      // new seed
        assertEquals(0, saved.round)       // reset
        assertEquals(0, saved.cursor)
    }

    @Test fun ensureInitialized_existingMatchingState_keepsIt() = runTest {
        val existing = selector.initialState(7L) // poolVersion matches selector
        val store = FakeStateSource(initial = existing)
        val repo = repo(store, seed = 999L) // seed must NOT be used

        repo.ensureInitialized()

        assertEquals(7L, store.classicsState().first()!!.seed)
    }

    @Test fun refresh_advancesCursorAndPersists() = runTest {
        val store = FakeStateSource()
        val repo = repo(store)

        repo.ensureInitialized()
        repo.refresh()

        val saved = store.saved!!
        assertEquals(0, saved.round)
        assertEquals(8, saved.cursor)
    }

    @Test fun restartRestoresCurrentBatchAndProgress() = runTest {
        val store = FakeStateSource()
        val bookmarks = FakeBookmarks()
        val repo1 = repo(store, bookmarks)
        repo1.ensureInitialized()
        repo1.refresh() // cursor 8

        // simulate a process restart: a fresh repository sharing the same persisted store
        val repo2 = repo(store, bookmarks, seed = 999L) // seed ignored; state restored
        repo2.ensureInitialized()

        val batchBefore = repo1.batchStream.first { it.isNotEmpty() }
        val batchAfter = repo2.batchStream.first { it.isNotEmpty() }
        assertEquals(batchBefore.map { it.id }, batchAfter.map { it.id })
        assertEquals(8, store.saved!!.cursor)
    }

    @Test fun batchStream_overlaysBookmarkState() = runTest {
        val bookmarks = FakeBookmarks()
        val repo = repo(bookmarks = bookmarks)
        repo.ensureInitialized()

        val firstBatch = repo.batchStream.first { it.isNotEmpty() }
        val targetId = firstBatch.first().id
        bookmarks.setBookmarked(targetId, true)

        val refreshed = repo.batchStream.first { it.isNotEmpty() }
        assertTrue(refreshed.first { it.id == targetId }.isBookmarked)
        assertEquals(1, refreshed.count { it.isBookmarked })
    }

    @Test fun bookmarkSurvivesRefresh() = runTest {
        val bookmarks = FakeBookmarks()
        val repo = repo(bookmarks = bookmarks)
        repo.ensureInitialized()

        val firstBatch = repo.batchStream.first { it.isNotEmpty() }
        val target = firstBatch.first()
        repo.toggleBookmark(target)
        assertTrue(bookmarks.ids.value.contains(target.id))

        repo.refresh() // advance to next batch; target leaves Classics

        val nextBatch = repo.batchStream.first { it.isNotEmpty() }
        assertFalse(nextBatch.any { it.id == target.id })          // no longer displayed
        assertTrue(bookmarks.ids.value.contains(target.id))        // but still bookmarked
    }

    private class FakeStateSource(initial: ClassicsState? = null) : ClassicsStateSource {
        private val flow = MutableStateFlow(initial)
        var saved: ClassicsState? = initial
            private set

        override fun classicsState(): Flow<ClassicsState?> = flow
        override suspend fun saveClassicsState(state: ClassicsState) {
            saved = state
            flow.value = state
        }
    }

    private class FakeBookmarks : Bookmarks {
        val ids = MutableStateFlow<Set<String>>(emptySet())
        override fun bookmarkedIdsStream(): Flow<Set<String>> = ids
        override suspend fun toggleBookmarkForArticle(article: Article) {
            ids.value = if (article.id in ids.value) ids.value - article.id else ids.value + article.id
        }
        fun setBookmarked(id: String, value: Boolean) {
            ids.value = if (value) ids.value + id else ids.value - id
        }
    }
}

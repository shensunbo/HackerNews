package com.example.hackernews.data.classics

import com.example.hackernews.data.repository.Bookmarks
import com.example.hackernews.domain.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

/**
 * Owns the offline classics batch lifecycle: hydrate persisted state on first
 * access, advance the cursor on refresh (wrapping into a reshuffled round when
 * exhausted), and overlay live bookmark state onto the current batch. Bookmark
 * writes are delegated to [Bookmarks] so a classic saved from Classics stays
 * openable from Bookmarks even after a refresh replaces the visible batch.
 */
class ClassicsRepository(
    private val selector: ClassicsSelector,
    private val stateStore: ClassicsStateSource,
    private val bookmarks: Bookmarks,
    private val seedGenerator: () -> Long = { Random.Default.nextLong() },
) {
    private val state = MutableStateFlow<ClassicsState?>(null)

    /** Number of items in the shipped pool (read-only, no initialization needed). */
    val poolSize: Int get() = selector.poolSize

    val batchStream: Flow<List<Article>> =
        combine(state, bookmarks.bookmarkedIdsStream()) { current, bookmarkedIds ->
            val active = current ?: return@combine emptyList()
            selector.batchFor(active).map { item ->
                val article = item.toArticle()
                article.copy(isBookmarked = article.id in bookmarkedIds)
            }
        }

    val metaStream: Flow<ClassicsMeta?> = state.map { it?.let(selector::metaFor) }

    suspend fun ensureInitialized() {
        if (state.value != null) return
        val stored = stateStore.classicsState().first()
        val initialized = when {
            stored == null -> selector.initialState(seedGenerator())
            stored.poolVersion != selector.poolVersion -> selector.initialState(seedGenerator())
            else -> stored
        }
        stateStore.saveClassicsState(initialized)
        state.value = initialized
    }

    suspend fun refresh() {
        val current = state.value ?: ensureInitializedAndReturn()
        val next = selector.refresh(current)
        stateStore.saveClassicsState(next)
        state.value = next
    }

    suspend fun toggleBookmark(article: Article) {
        bookmarks.toggleBookmarkForArticle(article)
    }

    private suspend fun ensureInitializedAndReturn(): ClassicsState {
        ensureInitialized()
        return state.value!!
    }
}

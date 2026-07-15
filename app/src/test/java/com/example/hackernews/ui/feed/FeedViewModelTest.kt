package com.example.hackernews.ui.feed

import com.example.hackernews.data.local.ReadingModeSource
import com.example.hackernews.data.repository.FeedDataSource
import com.example.hackernews.data.repository.RefreshResult
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun failedInitialRefresh_showsCachedContentMessage() = runTest(dispatcher) {
        val repository = FakeFeedDataSource(refreshResult = RefreshResult(0, failed = true))

        val viewModel = FeedViewModel(repository, FakeReadingModeSource())
        advanceUntilIdle()

        assertEquals("刷新失败，显示缓存", viewModel.error.value)
        assertFalse(viewModel.refreshing.value)
    }

    @Test fun refreshException_isReportedAndRefreshingStops() = runTest(dispatcher) {
        val repository = FakeFeedDataSource(refreshFailure = IllegalStateException("database"))

        val viewModel = FeedViewModel(repository, FakeReadingModeSource())
        advanceUntilIdle()

        assertEquals("刷新失败，显示缓存", viewModel.error.value)
        assertFalse(viewModel.refreshing.value)
    }

    private class FakeReadingModeSource : ReadingModeSource {
        override fun readingMode(): Flow<ReadingMode> = flowOf(ReadingMode.CUSTOM_TABS)
    }

    private class FakeFeedDataSource(
        private val refreshResult: RefreshResult = RefreshResult(0, failed = false),
        private val refreshFailure: Throwable? = null,
    ) : FeedDataSource {
        private val articles = MutableStateFlow<List<Article>>(emptyList())

        override fun feedStream(): Flow<List<Article>> = articles

        override suspend fun refresh(): RefreshResult {
            refreshFailure?.let { throw it }
            return refreshResult
        }

        override suspend fun toggleBookmark(id: String) = Unit
    }
}

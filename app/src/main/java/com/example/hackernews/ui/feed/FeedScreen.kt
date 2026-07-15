package com.example.hackernews.ui.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.hackernews.ui.components.ArticleRow
import com.example.hackernews.ui.components.BrailleSpinner
import com.example.hackernews.ui.components.EmptyState
import com.example.hackernews.ui.components.StatusBanner
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.util.LinkOpener
import com.example.hackernews.ui.util.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen() {
    val viewModel = appViewModel {
        FeedViewModel(it.feedRepository, it.preferencesStore)
    }
    val articles by viewModel.articles.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val context = LocalContext.current
    val nowMillis = remember { System.currentTimeMillis() }

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> dev_feed --sort=hot")
        error?.let { message ->
            StatusBanner(message = message, onRetry = viewModel::refresh)
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                articles.isEmpty() && refreshing -> BrailleSpinner("fetching feeds…")
                articles.isEmpty() -> EmptyState("~ 还没有内容，下拉刷新 ~")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(articles, key = { it.id }) { article ->
                        ArticleRow(
                            article = article,
                            nowMillis = nowMillis,
                            onOpen = {
                                LinkOpener.open(context, article.url, readingMode)
                            },
                            onToggleBookmark = {
                                viewModel.toggleBookmark(article.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

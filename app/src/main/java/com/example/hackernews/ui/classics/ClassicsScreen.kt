package com.example.hackernews.ui.classics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.hackernews.R
import com.example.hackernews.ui.components.ArticleRow
import com.example.hackernews.ui.components.BrailleSpinner
import com.example.hackernews.ui.components.EmptyState
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.theme.TerminalColors
import com.example.hackernews.ui.util.LinkOpener
import com.example.hackernews.ui.util.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicsScreen() {
    val viewModel = appViewModel {
        ClassicsViewModel(it.classicsRepository, it.preferencesStore)
    }
    val articles by viewModel.articles.collectAsState()
    val meta by viewModel.meta.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar(
            command = "> classics --pool",
            action = {
                RefreshAction(onClick = viewModel::refresh)
            },
        )
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                articles.isEmpty() && meta == null -> BrailleSpinner("loading classics…")
                articles.isEmpty() -> EmptyState("~ pool empty, pull to reshuffle ~")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(articles, key = { it.id }) { article ->
                        ArticleRow(
                            article = article,
                            nowMillis = 0L,
                            showTime = false,
                            onOpen = { LinkOpener.open(context, article.url, readingMode) },
                            onToggleBookmark = { viewModel.toggleBookmark(article) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = "Refresh classics",
            tint = TerminalColors.Primary,
        )
    }
}

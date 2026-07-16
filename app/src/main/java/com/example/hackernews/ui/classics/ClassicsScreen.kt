package com.example.hackernews.ui.classics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.hackernews.data.classics.ClassicsMeta
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    meta?.let { ProgressLabel(it) }
                    RefreshAction(onClick = viewModel::refresh)
                }
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
private fun ProgressLabel(meta: ClassicsMeta) {
    Text(
        text = "r${meta.round + 1}·${meta.batchIndex + 1}/${meta.totalBatches}",
        color = TerminalColors.PrimaryDim,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun RefreshAction(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "[refresh]",
            color = TerminalColors.Primary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

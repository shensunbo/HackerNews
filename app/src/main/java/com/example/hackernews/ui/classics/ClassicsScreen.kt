package com.example.hackernews.ui.classics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.hackernews.ui.components.ArticleRow
import com.example.hackernews.ui.components.EmptyState
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.util.LinkOpener
import com.example.hackernews.ui.util.appViewModel

@Composable
fun ClassicsScreen() {
    val viewModel = appViewModel {
        ClassicsViewModel(it.feedRepository, it.preferencesStore, it.classics)
    }
    val articles by viewModel.articles.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> classics --must_read")
        if (articles.isEmpty()) {
            EmptyState("$ classics.json is empty")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(articles, key = { it.id }) { article ->
                    ArticleRow(
                        article = article,
                        nowMillis = 0L,
                        showTime = false,
                        onOpen = {
                            LinkOpener.open(context, article.url, readingMode)
                        },
                        onToggleBookmark = {
                            viewModel.toggleBookmark(article)
                        },
                    )
                }
            }
        }
    }
}

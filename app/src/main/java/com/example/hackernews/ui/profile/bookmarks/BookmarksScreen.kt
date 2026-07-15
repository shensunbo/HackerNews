package com.example.hackernews.ui.profile.bookmarks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.hackernews.domain.model.ArticleOrigin
import com.example.hackernews.ui.components.ArticleRow
import com.example.hackernews.ui.components.EmptyState
import com.example.hackernews.ui.components.SearchField
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.components.TopicChipRow
import com.example.hackernews.ui.util.LinkOpener
import com.example.hackernews.ui.util.appViewModel

@Composable
fun BookmarksScreen(onBack: () -> Unit) {
    val viewModel = appViewModel {
        BookmarksViewModel(it.feedRepository, it.preferencesStore)
    }
    val articles by viewModel.articles.collectAsState()
    val chips by viewModel.topicChips.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val query by viewModel.query.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val context = LocalContext.current
    val nowMillis = remember { System.currentTimeMillis() }

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> bookmarks", onBack = onBack)
        SearchField(value = query, onValueChange = viewModel::setQuery)
        TopicChipRow(
            chips = chips,
            selectedId = selectedTopic,
            onSelect = viewModel::setTopic,
        )
        if (articles.isEmpty()) {
            val hasFilter = query.isNotBlank() || selectedTopic != null
            EmptyState(if (hasFilter) "$ no match" else "$ no bookmarks yet")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(articles, key = { it.id }) { article ->
                    ArticleRow(
                        article = article,
                        nowMillis = nowMillis,
                        showTime = article.origin != ArticleOrigin.CLASSIC,
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

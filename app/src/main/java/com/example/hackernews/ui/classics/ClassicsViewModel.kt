package com.example.hackernews.ui.classics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.config.ClassicItem
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.remote.articleIdFor
import com.example.hackernews.data.repository.FeedRepository
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ArticleOrigin
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun ClassicItem.toArticle(): Article = Article(
    id = articleIdFor(url),
    title = title,
    url = url,
    summary = summary,
    source = "Must Read",
    topicIds = topicId.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty(),
    publishedAt = 0L,
    origin = ArticleOrigin.CLASSIC,
)

class ClassicsViewModel(
    private val repository: FeedRepository,
    preferencesStore: PreferencesStore,
    classics: List<ClassicItem>,
) : ViewModel() {
    private val baseArticles = classics.map(ClassicItem::toArticle)

    val articles = repository.bookmarkedIdsStream()
        .map { bookmarkedIds ->
            baseArticles.map { article ->
                article.copy(isBookmarked = article.id in bookmarkedIds)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = baseArticles,
        )
    val readingMode = preferencesStore.readingMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReadingMode.CUSTOM_TABS,
    )

    fun toggleBookmark(article: Article) = viewModelScope.launch {
        repository.toggleBookmarkForArticle(article)
    }
}

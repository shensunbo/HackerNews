package com.example.hackernews.ui.profile.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.repository.FeedRepository
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModel(
    private val repository: FeedRepository,
    preferencesStore: PreferencesStore,
) : ViewModel() {
    val selectedTopic = MutableStateFlow<String?>(null)
    val query = MutableStateFlow("")
    val topicChips = repository.topicsStream()
        .map { topics -> topics.map { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val readingMode = preferencesStore.readingMode()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReadingMode.CUSTOM_TABS,
        )
    val articles = combine(selectedTopic, query, ::Pair)
        .flatMapLatest { (topicId, query) ->
            repository.bookmarksStream(topicId, query)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList<Article>(),
        )

    fun setTopic(id: String?) {
        selectedTopic.value = id
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleBookmark(id: String) = viewModelScope.launch {
        repository.toggleBookmark(id)
    }
}

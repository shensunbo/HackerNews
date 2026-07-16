package com.example.hackernews.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.local.ReadingModeSource
import com.example.hackernews.data.repository.FeedDataSource
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: FeedDataSource,
    readingModeSource: ReadingModeSource,
) : ViewModel() {
    val articles = repository.feedStream().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList<Article>(),
    )
    val readingMode = readingModeSource.readingMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReadingMode.CUSTOM_TABS,
    )
    val refreshing = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        if (refreshing.value) return@launch
        refreshing.value = true
        try {
            val result = repository.refresh()
            error.value = if (result.failed) "refresh failed — showing cache" else null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            error.value = "refresh failed — showing cache"
        } finally {
            refreshing.value = false
        }
    }

    fun toggleBookmark(id: String) = viewModelScope.launch {
        repository.toggleBookmark(id)
    }
}

package com.example.hackernews.ui.classics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.classics.ClassicsRepository
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClassicsViewModel(
    private val repository: ClassicsRepository,
    preferencesStore: PreferencesStore,
) : ViewModel() {
    val articles = repository.batchStream.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    val meta = repository.metaStream.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
    val readingMode = preferencesStore.readingMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReadingMode.CUSTOM_TABS,
    )
    val refreshing = MutableStateFlow(false)

    init {
        viewModelScope.launch { repository.ensureInitialized() }
    }

    fun refresh() = viewModelScope.launch {
        if (refreshing.value) return@launch
        refreshing.value = true
        try {
            repository.refresh()
        } finally {
            refreshing.value = false
        }
    }

    fun toggleBookmark(article: Article) = viewModelScope.launch {
        repository.toggleBookmark(article)
    }
}

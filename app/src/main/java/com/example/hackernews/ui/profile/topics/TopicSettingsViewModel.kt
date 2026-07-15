package com.example.hackernews.ui.profile.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.repository.FeedRepository
import com.example.hackernews.domain.model.Topic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicSettingsViewModel(
    repository: FeedRepository,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {
    val topics = repository.topicsStream().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList<Topic>(),
    )

    fun setEnabled(id: String, enabled: Boolean) = viewModelScope.launch {
        preferencesStore.setEnabled(id, enabled)
    }

    fun setWeight(id: String, weight: Float) = viewModelScope.launch {
        preferencesStore.setWeight(id, weight)
    }
}

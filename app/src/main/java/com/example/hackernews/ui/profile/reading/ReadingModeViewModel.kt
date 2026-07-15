package com.example.hackernews.ui.profile.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReadingModeViewModel(
    private val preferencesStore: PreferencesStore,
) : ViewModel() {
    val mode = preferencesStore.readingMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReadingMode.CUSTOM_TABS,
    )

    fun set(mode: ReadingMode) = viewModelScope.launch {
        preferencesStore.setReadingMode(mode)
    }
}

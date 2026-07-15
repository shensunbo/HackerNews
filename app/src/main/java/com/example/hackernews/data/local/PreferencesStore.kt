package com.example.hackernews.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "prefs")

interface TopicPreferencesSource {
    fun topicPrefs(): Flow<Map<String, PreferencesStore.TopicPref>>
}

interface ReadingModeSource {
    fun readingMode(): Flow<ReadingMode>
}

class PreferencesStore(private val context: Context) : TopicPreferencesSource, ReadingModeSource {

    data class TopicPref(val enabled: Boolean?, val weight: Float?)

    private val readingModeKey = stringPreferencesKey("reading_mode")
    private fun enabledKey(id: String) = booleanPreferencesKey("topic_enabled_$id")
    private fun weightKey(id: String) = floatPreferencesKey("topic_weight_$id")

    override fun readingMode(): Flow<ReadingMode> = context.dataStore.data.map { p ->
        when (p[readingModeKey]) {
            ReadingMode.EXTERNAL_BROWSER.name -> ReadingMode.EXTERNAL_BROWSER
            else -> ReadingMode.CUSTOM_TABS
        }
    }
    suspend fun setReadingMode(mode: ReadingMode) {
        context.dataStore.edit { it[readingModeKey] = mode.name }
    }

    override fun topicPrefs(): Flow<Map<String, TopicPref>> = context.dataStore.data.map { p ->
        val ids = p.asMap().keys
            .mapNotNull { k ->
                when {
                    k.name.startsWith("topic_enabled_") -> k.name.removePrefix("topic_enabled_")
                    k.name.startsWith("topic_weight_") -> k.name.removePrefix("topic_weight_")
                    else -> null
                }
            }.toSet()
        ids.associateWith { id ->
            TopicPref(
                enabled = p[enabledKey(id)],
                weight = p[weightKey(id)],
            )
        }
    }
    suspend fun setEnabled(topicId: String, enabled: Boolean) {
        context.dataStore.edit { it[enabledKey(topicId)] = enabled }
    }
    suspend fun setWeight(topicId: String, weight: Float) {
        context.dataStore.edit { it[weightKey(topicId)] = weight }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

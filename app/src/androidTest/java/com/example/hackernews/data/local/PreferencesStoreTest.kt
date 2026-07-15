package com.example.hackernews.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesStoreTest {
    private val store = PreferencesStore(ApplicationProvider.getApplicationContext())

    @Before fun reset() = runBlocking { store.clear() }   // isolate persistent DataStore per test

    @Test fun readingMode_defaultsToCustomTabs() = runBlocking {
        assertEquals(ReadingMode.CUSTOM_TABS, store.readingMode().first())
    }
    @Test fun readingMode_persists() = runBlocking {
        store.setReadingMode(ReadingMode.EXTERNAL_BROWSER)
        assertEquals(ReadingMode.EXTERNAL_BROWSER, store.readingMode().first())
    }
    @Test fun topicPref_roundTrip() = runBlocking {
        store.setEnabled("ai", false)
        store.setWeight("ai", 1.5f)
        val pref = store.topicPrefs().first()["ai"]!!
        assertEquals(false, pref.enabled)
        assertEquals(1.5f, pref.weight)
    }

    @Test fun settingEnabled_doesNotOverrideConfiguredWeight() = runBlocking {
        store.setEnabled("backend", false)

        val pref = store.topicPrefs().first()["backend"]!!
        assertEquals(false, pref.enabled)
        assertNull(pref.weight)
    }

    @Test fun settingWeight_doesNotOverrideConfiguredEnabledState() = runBlocking {
        store.setWeight("experimental", 1.5f)

        val pref = store.topicPrefs().first()["experimental"]!!
        assertNull(pref.enabled)
        assertEquals(1.5f, pref.weight)
    }

    @Test fun topicWeight_isClampedToSupportedRange() = runBlocking {
        store.setWeight("ai", 3.5f)
        assertEquals(2.0f, store.topicPrefs().first()["ai"]!!.weight)

        store.setWeight("ai", -1.0f)
        assertEquals(0.0f, store.topicPrefs().first()["ai"]!!.weight)
    }
}

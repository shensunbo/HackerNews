package com.example.hackernews.ui.util

import com.example.hackernews.domain.model.ReadingMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkOpenerDecisionTest {
    @Test fun customTabsWhenPreferredAndAvailable() {
        assertTrue(shouldUseCustomTabs(ReadingMode.CUSTOM_TABS, providerAvailable = true))
    }

    @Test fun fallbackToExternalWhenNoProvider() {
        assertFalse(shouldUseCustomTabs(ReadingMode.CUSTOM_TABS, providerAvailable = false))
    }

    @Test fun externalWhenPreferred() {
        assertFalse(shouldUseCustomTabs(ReadingMode.EXTERNAL_BROWSER, providerAvailable = true))
    }
}

package com.example.hackernews.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.CompositionLocalProvider
import com.example.hackernews.ui.components.LocalTerminalAnimationsEnabled
import com.example.hackernews.ui.theme.HackerNewsTheme
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

// Quarantined diagnostic test: Compose UI instrumented test hangs on the target vivo device
// (Compose/Espresso idling-sync issue). Out of the plan's scope; kept for later triage.
@Ignore("Compose/Espresso idling hang on device; diagnostic only.")
class ProfileScreenIdleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileScreenCanDisableDecorativeAnimations() {
        composeRule.setContent {
            HackerNewsTheme {
                CompositionLocalProvider(LocalTerminalAnimationsEnabled provides false) {
                    ProfileScreen({}, {}, {}, {})
                }
            }
        }

        composeRule.onNodeWithText("★  收藏").assertIsDisplayed()
    }
}

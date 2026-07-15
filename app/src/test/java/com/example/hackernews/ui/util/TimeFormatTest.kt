package com.example.hackernews.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    private val now = 10_000_000_000L

    @Test fun justNow() = assertEquals("just now", relativeTime(now - 30_000, now))

    @Test fun minutes() = assertEquals("5m ago", relativeTime(now - 5 * 60_000, now))

    @Test fun hours() = assertEquals("3h ago", relativeTime(now - 3 * 3_600_000, now))

    @Test fun days() = assertEquals("2d ago", relativeTime(now - 2 * 86_400_000, now))
}

package com.example.hackernews.ui.util

fun relativeTime(pastMillis: Long, nowMillis: Long): String {
    val minutes = (nowMillis - pastMillis).coerceAtLeast(0L) / 60_000L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1_440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1_440}d ago"
    }
}

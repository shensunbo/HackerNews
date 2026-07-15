package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Topic

fun matchTopics(text: String, topics: List<Topic>): List<String> {
    val lower = text.lowercase()
    return topics.filter { t ->
        t.keywords.any { kw -> kw.isNotBlank() && lower.contains(kw.lowercase()) }
    }.map { it.id }
}

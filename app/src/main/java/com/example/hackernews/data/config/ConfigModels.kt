package com.example.hackernews.data.config

import com.example.hackernews.domain.model.Topic
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TopicDto(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val weight: Float = 1.0f,
    val feeds: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
)

@Serializable
data class TopicsConfig(val topics: List<TopicDto> = emptyList())

@Serializable
data class ClassicItem(
    val title: String,
    val url: String,
    val summary: String = "",
    val topicId: String = "",
)

@Serializable
data class ClassicsConfig(
    val poolVersion: Int = 1,
    val items: List<ClassicItem> = emptyList(),
)

/** Parsed classics.json: a versioned pool of must-read articles. */
data class ClassicsPool(
    val poolVersion: Int,
    val items: List<ClassicItem>,
)

private val configJson = Json { ignoreUnknownKeys = true }

fun parseTopics(json: String): List<Topic> =
    configJson.decodeFromString<TopicsConfig>(json).topics.map {
        Topic(it.id, it.name, it.enabled, it.weight, it.feeds, it.keywords)
    }

fun parseClassicsPool(json: String): ClassicsPool {
    val config = configJson.decodeFromString<ClassicsConfig>(json)
    return ClassicsPool(config.poolVersion, config.items)
}

fun parseClassics(json: String): List<ClassicItem> = parseClassicsPool(json).items

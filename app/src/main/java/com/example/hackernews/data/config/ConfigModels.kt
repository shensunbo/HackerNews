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
data class ClassicsConfig(val items: List<ClassicItem> = emptyList())

private val configJson = Json { ignoreUnknownKeys = true }

fun parseTopics(json: String): List<Topic> =
    configJson.decodeFromString<TopicsConfig>(json).topics.map {
        Topic(it.id, it.name, it.enabled, it.weight, it.feeds, it.keywords)
    }

fun parseClassics(json: String): List<ClassicItem> =
    configJson.decodeFromString<ClassicsConfig>(json).items

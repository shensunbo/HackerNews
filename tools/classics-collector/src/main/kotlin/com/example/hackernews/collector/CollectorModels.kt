package com.example.hackernews.collector

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ClassicJsonItem(
    val title: String,
    val url: String,
    val summary: String,
    val topicId: String,
)

@Serializable
data class ClassicsFile(
    val poolVersion: Int = 1,
    val items: List<ClassicJsonItem> = emptyList(),
)

@Serializable
private data class TopicDto(
    val id: String,
    val keywords: List<String> = emptyList(),
    val feeds: List<String> = emptyList(),
)

@Serializable
private data class TopicsFile(val topics: List<TopicDto> = emptyList())

@Serializable
data class HnItem(
    val id: Long,
    val type: String? = null,
    val title: String? = null,
    val url: String? = null,
    val score: Int? = null,
    val time: Long? = null,
)

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
internal val parseJson = Json { ignoreUnknownKeys = true }

fun parseTopicsForCollector(jsonText: String): List<Pair<CollectorTopic, List<String>>> =
    parseJson.decodeFromString<TopicsFile>(jsonText).topics.map {
        CollectorTopic(it.id, it.keywords) to it.feeds
    }

fun parseExistingPoolVersion(jsonText: String): Int? =
    runCatching { parseJson.decodeFromString<ClassicsFile>(jsonText).poolVersion }.getOrNull()

fun encodeClassics(poolVersion: Int, items: List<CollectorItem>): String {
    val file = ClassicsFile(
        poolVersion = poolVersion,
        items = items.map { ClassicJsonItem(it.title, it.url, it.summary, it.topicId) },
    )
    return json.encodeToString(ClassicsFile.serializer(), file)
}

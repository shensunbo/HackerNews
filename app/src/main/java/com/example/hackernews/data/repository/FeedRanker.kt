package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article
import kotlin.math.min
import kotlin.math.pow

fun rankScore(a: Article, topicWeights: Map<String, Float>, nowMillis: Long): Double {
    val weight = a.topicIds.maxOfOrNull { (topicWeights[it] ?: 1.0f).toDouble() } ?: 1.0
    val ageHours = ((nowMillis - a.publishedAt).coerceAtLeast(0L)) / 3_600_000.0
    val recency = 0.5.pow(ageHours / 24.0)           // 半衰期 24 小时
    val scoreBoost = a.score?.let { min(it / 300.0, 1.0) * 0.25 } ?: 0.0
    return weight * recency + scoreBoost
}

fun rankFeed(list: List<Article>, topicWeights: Map<String, Float>, nowMillis: Long): List<Article> =
    list.sortedByDescending { rankScore(it, topicWeights, nowMillis) }

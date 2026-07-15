package com.example.hackernews.domain.model

data class Topic(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val weight: Float,           // 0.0–2.0，默认 1.0
    val feeds: List<String>,
    val keywords: List<String>,
)

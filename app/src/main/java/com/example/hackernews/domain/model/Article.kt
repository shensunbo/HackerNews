package com.example.hackernews.domain.model

data class Article(
    val id: String,              // 规范化 URL 的稳定 hash
    val title: String,
    val url: String,
    val summary: String,         // RSS 简介；HN 可能为空
    val source: String,          // 来源名
    val topicIds: List<String>,
    val publishedAt: Long,       // epoch millis
    val isBookmarked: Boolean = false,
    val score: Int? = null,      // HN 分数
    val origin: ArticleOrigin = ArticleOrigin.FEED,
)

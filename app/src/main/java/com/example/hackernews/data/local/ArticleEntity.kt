package com.example.hackernews.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hackernews.domain.model.ArticleOrigin

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val summary: String,
    val source: String,
    val topicIds: String,        // 逗号分隔
    val publishedAt: Long,
    val isBookmarked: Boolean,
    val score: Int?,
    val origin: ArticleOrigin = ArticleOrigin.FEED,
)

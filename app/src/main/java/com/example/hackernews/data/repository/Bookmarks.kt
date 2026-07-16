package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article
import kotlinx.coroutines.flow.Flow

/** Bookmark access for screens that overlay bookmark state on non-feed articles. */
interface Bookmarks {
    fun bookmarkedIdsStream(): Flow<Set<String>>
    suspend fun toggleBookmarkForArticle(article: Article)
}

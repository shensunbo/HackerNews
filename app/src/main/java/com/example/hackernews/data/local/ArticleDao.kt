package com.example.hackernews.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun feedStream(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY publishedAt DESC")
    fun bookmarksStream(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: String): ArticleEntity?

    @Query("UPDATE articles SET isBookmarked = :value WHERE id = :id")
    suspend fun setBookmarked(id: String, value: Boolean)

    @Query("SELECT isBookmarked FROM articles WHERE id = :id")
    suspend fun bookmarkFlag(id: String): Boolean?

    @Query("""INSERT OR REPLACE INTO articles
        (id,title,url,summary,source,topicIds,publishedAt,isBookmarked,score)
        VALUES (:id,:title,:url,:summary,:source,:topicIds,:publishedAt,:isBookmarked,:score)""")
    suspend fun insertRaw(
        id: String, title: String, url: String, summary: String, source: String,
        topicIds: String, publishedAt: Long, isBookmarked: Boolean, score: Int?,
    )

    @Transaction
    suspend fun upsertPreservingBookmark(items: List<ArticleEntity>) {
        for (e in items) {
            val kept = bookmarkFlag(e.id) ?: false
            insertRaw(e.id, e.title, e.url, e.summary, e.source, e.topicIds,
                e.publishedAt, kept, e.score)
        }
    }
}

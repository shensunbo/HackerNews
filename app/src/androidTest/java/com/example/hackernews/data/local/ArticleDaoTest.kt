package com.example.hackernews.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ArticleDao

    private fun entity(id: String, bm: Boolean = false) =
        ArticleEntity(id, "t-$id", "https://e.com/$id", "s", "src", "backend", 1000L, bm, null)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).build()
        dao = db.articleDao()
    }
    @After fun close() = db.close()

    @Test fun upsertThenBookmark_preservedOnReUpsert() = runBlocking {
        dao.upsertPreservingBookmark(listOf(entity("a")))
        dao.setBookmarked("a", true)
        dao.upsertPreservingBookmark(listOf(entity("a")))          // 重新抓取同一条
        assertTrue(dao.getById("a")!!.isBookmarked)                 // 收藏未被覆盖
    }
    @Test fun bookmarksStream_onlyBookmarked() = runBlocking {
        dao.upsertPreservingBookmark(listOf(entity("a"), entity("b")))
        dao.setBookmarked("b", true)
        val list = dao.bookmarksStream().first()
        assertEquals(1, list.size)
        assertEquals("b", list[0].id)
    }
}

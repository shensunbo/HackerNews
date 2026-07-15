package com.example.hackernews.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ArticleMapperTest {
    @Test fun normalize_stripsFragmentTrailingSlashAndTracking() {
        assertEquals(
            "https://ex.com/post",
            normalizeUrl("HTTPS://Ex.com/post/?utm_source=hn&ref=x#top")
        )
    }
    @Test fun normalize_keepsMeaningfulQuery() {
        assertEquals("https://ex.com/p?id=42", normalizeUrl("https://ex.com/p?id=42"))
    }
    @Test fun id_isStableAcrossEquivalentUrls() {
        assertEquals(articleIdFor("https://ex.com/a#x"), articleIdFor("https://ex.com/a/"))
    }
    @Test fun id_differsForDifferentUrls() {
        assertNotEquals(articleIdFor("https://ex.com/a"), articleIdFor("https://ex.com/b"))
    }
}

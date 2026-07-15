package com.example.hackernews.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RssFormatTest {
    @Test fun parsesRfc1123() {
        assertEquals(1727874000000L, parseRssDateMillis("Wed, 02 Oct 2024 13:00:00 GMT"))
    }
    @Test fun parsesIso8601() {
        assertEquals(1727874000000L, parseRssDateMillis("2024-10-02T13:00:00Z"))
    }
    @Test fun invalidDateReturnsNull() {
        assertNull(parseRssDateMillis("not a date"))
        assertNull(parseRssDateMillis(null))
    }
    @Test fun stripHtml_removesTagsAndEntities() {
        assertEquals("A & B link", stripHtml("<p>A &amp; B <a href='x'>link</a></p>"))
    }
    @Test fun stripHtml_blankInput() {
        assertEquals("", stripHtml(null))
    }
    @Test fun hostOf_extractsHost() {
        assertEquals("martinfowler.com", hostOf("https://martinfowler.com/feed.atom"))
    }
}

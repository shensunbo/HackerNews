package com.example.hackernews.collector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectorTest {
    private val topics = listOf(
        CollectorTopic("backend", listOf("database", "architecture")),
        CollectorTopic("frontend", listOf("react", "css")),
    )
    private val validTopicIds = topics.map { it.id }.toSet()

    @Test fun normalizeUrl_lowercasesHostAndStripsTracking() {
        assertEquals(
            "https://example.com/path",
            normalizeUrl("https://Example.com/path/?utm_source=x#frag"),
        )
    }

    @Test fun articleIdFor_isStableAnd16HexChars() {
        val id = articleIdFor("https://example.com/a")
        assertEquals(16, id.length)
        assertTrue(id.matches(Regex("^[0-9a-f]{16}$")))
        assertEquals(id, articleIdFor("https://example.com/a"))
    }

    @Test fun classifyTopic_returnsFirstMatchOrNull() {
        assertEquals("backend", classifyTopic("a database architecture post", topics))
        assertEquals("frontend", classifyTopic("react css tips", topics))
        assertNull(classifyTopic("a recipe for pasta", topics))
    }

    @Test fun dedupeAndClean_mergesBlanksAndDropsInvalid() {
        val a = articleIdFor("https://x.io/a")
        val items = listOf(
            CollectorItem("Title A", "https://x.io/a", "", "backend", "src"),        // summary blank
            CollectorItem("", "https://x.io/a", "summary A", "backend", "src2"),     // title blank, same id -> merge
            CollectorItem("Title B", "https://x.io/b", "summary B", "unknown", "s"), // invalid topic
            CollectorItem("Title C", "https://x.io/c", "summary C", "frontend", "s"),
        )

        val cleaned = dedupeAndClean(items, validTopicIds)

        assertEquals(2, cleaned.size)
        // merged A: title + summary both filled from siblings
        val mergedA = cleaned.first { articleIdFor(it.url) == a }
        assertEquals("Title A", mergedA.title)
        assertEquals("summary A", mergedA.summary)
    }

    @Test fun dedupeAndClean_dropsNonHttpUrls() {
        val items = listOf(
            CollectorItem("T", "ftp://x.io/a", "s", "backend", "src"),
            CollectorItem("T2", "https://x.io/b", "s", "backend", "src"),
        )
        val cleaned = dedupeAndClean(items, validTopicIds)
        assertEquals(1, cleaned.size)
        assertEquals("https://x.io/b", cleaned[0].url)
    }

    @Test fun selfCheck_passesForEnoughValidItems() {
        val items = (0 until 480).map {
            CollectorItem("t$it", "https://x.io/$it", "s$it", "backend", "src")
        }
        assertEquals(emptyList<String>(), selfCheck(items, validTopicIds, minCount = 480))
    }

    @Test fun selfCheck_failsWhenUnderTarget() {
        val items = (0 until 100).map {
            CollectorItem("t$it", "https://x.io/$it", "s$it", "backend", "src")
        }
        val errors = selfCheck(items, validTopicIds, minCount = 480)
        assertTrue(errors.any { it.contains("100") && it.contains("480") })
    }

    @Test fun selfCheck_flagsDuplicatesBlanksAndBadTopics() {
        val dupUrl = "https://x.io/dup"
        val items = listOf(
            CollectorItem("t1", dupUrl, "s1", "backend", "src"),
            CollectorItem("t2", dupUrl, "s2", "backend", "src"), // duplicate id
            CollectorItem("", "https://x.io/blank", "s", "backend", "src"), // blank title
            CollectorItem("t4", "https://x.io/bad", "s", "nope", "src"), // bad topic
        )
        val errors = selfCheck(items, validTopicIds, minCount = 1)
        assertTrue(errors.any { it.contains("duplicate article ids") })
        assertTrue(errors.any { it.contains("blank title") })
        assertTrue(errors.any { it.contains("unknown topicId nope") })
    }

    @Test fun nextPoolVersion_bumpsExistingOrDefaults() {
        assertEquals(3, nextPoolVersion(2))
        assertEquals(2, nextPoolVersion(null))
    }

    @Test fun encodeClassics_roundTripsThroughClassicsFile() {
        val items = listOf(
            CollectorItem("Title", "https://x.io/a", "summary", "backend", "src"),
        )
        val encoded = encodeClassics(poolVersion = 3, items = items)
        val parsed = parseJson.decodeFromString(ClassicsFile.serializer(), encoded)

        assertEquals(3, parsed.poolVersion)
        assertEquals(1, parsed.items.size)
        assertEquals("Title", parsed.items[0].title)
        assertEquals("backend", parsed.items[0].topicId)
    }

    @Test fun parseExistingPoolVersion_readsVersionOrDefaultsNull() {
        assertEquals(2, parseExistingPoolVersion("""{"poolVersion":2,"items":[]}"""))
        assertNull(parseExistingPoolVersion("not json"))
    }
}

package com.example.hackernews.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigParsingTest {
    @Test fun parsesTopicsWithDefaults() {
        val json = """
          {"topics":[{"id":"backend","name":"后端 & 架构","enabled":true,"weight":1.0,
          "feeds":["https://martinfowler.com/feed.atom"],"keywords":["database","architecture"]}]}
        """.trimIndent()
        val topics = parseTopics(json)
        assertEquals(1, topics.size)
        assertEquals("backend", topics[0].id)
        assertEquals(1.0f, topics[0].weight)
        assertTrue(topics[0].enabled)
        assertEquals(listOf("database", "architecture"), topics[0].keywords)
    }

    @Test fun parsesClassics() {
        val json = """
          {"items":[{"title":"The Twelve-Factor App","url":"https://12factor.net",
          "summary":"云原生 12 准则","topicId":"backend"}]}
        """.trimIndent()
        val items = parseClassics(json)
        assertEquals(1, items.size)
        assertEquals("https://12factor.net", items[0].url)
        assertEquals("backend", items[0].topicId)
    }

    @Test fun ignoresUnknownKeys() {
        val json = """{"topics":[{"id":"x","name":"X","enabled":false,"weight":0.5,
          "feeds":[],"keywords":[],"future":"ignored"}],"meta":"ignored"}"""
        assertEquals("x", parseTopics(json)[0].id)
    }
}

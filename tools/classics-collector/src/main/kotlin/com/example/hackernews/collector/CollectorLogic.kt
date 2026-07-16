package com.example.hackernews.collector

import java.security.MessageDigest

// The URL-normalization, id-derivation, HTML-stripping and keyword-matching
// functions below mirror the runtime's data/remote/ArticleMapper.kt and
// KeywordMatcher.kt so the ids the collector emits are byte-identical to the
// ones the app computes at runtime. If those runtime helpers change, update
// these mirrors too (or extract a shared module).

private val TRACKING_PARAMS = setOf("ref", "fbclid", "gclid")

fun normalizeUrl(url: String): String {
    val trimmed = url.trim()
    val hashIdx = trimmed.indexOf('#')
    val noFragment = if (hashIdx >= 0) trimmed.substring(0, hashIdx) else trimmed
    val qIdx = noFragment.indexOf('?')
    val base = if (qIdx >= 0) noFragment.substring(0, qIdx) else noFragment
    val query = if (qIdx >= 0) noFragment.substring(qIdx + 1) else ""

    val schemeSplit = base.split("://", limit = 2)
    val normalizedBase = if (schemeSplit.size == 2) {
        val rest = schemeSplit[1]
        val slash = rest.indexOf('/')
        val host = (if (slash >= 0) rest.substring(0, slash) else rest).lowercase()
        val path = if (slash >= 0) rest.substring(slash) else ""
        "${schemeSplit[0].lowercase()}://$host$path"
    } else base
    val trimmedBase = normalizedBase.trimEnd('/')

    val keptParams = query.split('&')
        .filter { it.isNotEmpty() }
        .filter { p ->
            val k = p.substringBefore('=')
            k.isNotEmpty() && !k.startsWith("utm_") && k !in TRACKING_PARAMS
        }
    return if (keptParams.isEmpty()) trimmedBase else "$trimmedBase?${keptParams.joinToString("&")}"
}

fun articleIdFor(url: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(normalizeUrl(url).toByteArray())
    return digest.take(8).joinToString("") { "%02x".format(it) }
}

fun stripHtml(input: String?): String {
    if (input.isNullOrBlank()) return ""
    return input
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&#39;", "'").replace("&quot;", "\"")
        .replace(Regex("\\s+"), " ").trim()
}

data class CollectorTopic(val id: String, val keywords: List<String>)

fun matchTopics(text: String, topics: List<CollectorTopic>): List<String> {
    val lower = text.lowercase()
    return topics.filter { t ->
        t.keywords.any { kw -> kw.isNotBlank() && lower.contains(kw.lowercase()) }
    }.map { it.id }
}

/** First topic whose keywords match [text], or null if none match. */
fun classifyTopic(text: String, topics: List<CollectorTopic>): String? =
    matchTopics(text, topics).firstOrNull()

data class CollectorItem(
    val title: String,
    val url: String,
    val summary: String,
    val topicId: String,
    val source: String,
)

/**
 * Merge items that share a normalized-URL id (filling blanks from siblings),
 * then drop items missing a title/summary, with an unknown topic, or a
 * non-http(s) URL. The result keeps the first occurrence of each id.
 */
fun dedupeAndClean(items: List<CollectorItem>, validTopicIds: Set<String>): List<CollectorItem> =
    items.groupBy { articleIdFor(it.url) }
        .values
        .map { group ->
            group.reduce { acc, x ->
                acc.copy(
                    title = acc.title.ifBlank { x.title },
                    summary = acc.summary.ifBlank { x.summary },
                    topicId = acc.topicId.ifBlank { x.topicId },
                )
            }
        }
        .filter { item ->
            item.title.isNotBlank() &&
                item.url.isNotBlank() &&
                item.summary.isNotBlank() &&
                item.topicId in validTopicIds &&
                (item.url.startsWith("http://") || item.url.startsWith("https://"))
        }

/** Returns the (possibly empty) list of self-check errors for a finished pool. */
fun selfCheck(items: List<CollectorItem>, validTopicIds: Set<String>, minCount: Int): List<String> {
    val errors = mutableListOf<String>()
    if (items.size < minCount) {
        errors += "pool has ${items.size} items, expected >= $minCount"
    }
    val ids = items.map { articleIdFor(it.url) }
    if (ids.size != ids.toSet().size) errors += "duplicate article ids present"
    val urls = items.map { normalizeUrl(it.url) }
    if (urls.size != urls.toSet().size) errors += "duplicate normalized URLs present"
    items.forEach { item ->
        if (item.title.isBlank()) errors += "blank title: ${item.url}"
        if (item.summary.isBlank()) errors += "blank summary: ${item.url}"
        if (item.topicId !in validTopicIds) errors += "unknown topicId ${item.topicId}: ${item.url}"
        if (!item.url.startsWith("http://") && !item.url.startsWith("https://")) {
            errors += "non-http(s) url: ${item.url}"
        }
    }
    return errors
}

/** Bump the shipped pool version so the runtime re-initializes on next launch. */
fun nextPoolVersion(existing: Int?): Int = (existing ?: 1) + 1

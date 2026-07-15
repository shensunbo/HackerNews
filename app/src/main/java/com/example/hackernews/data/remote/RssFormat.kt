package com.example.hackernews.data.remote

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun parseRssDateMillis(raw: String?): Long? {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty()) return null
    return runCatching {
        ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    }.recoverCatching {
        OffsetDateTime.parse(s).toInstant().toEpochMilli()
    }.recoverCatching {
        Instant.parse(s).toEpochMilli()
    }.getOrNull()
}

fun stripHtml(input: String?): String {
    if (input.isNullOrBlank()) return ""
    return input
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&#39;", "'").replace("&quot;", "\"")
        .replace(Regex("\\s+"), " ").trim()
}

fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull().orEmpty()

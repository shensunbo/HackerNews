package com.example.hackernews.data.remote

import java.security.MessageDigest

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

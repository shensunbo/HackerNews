package com.example.hackernews.collector

import com.prof18.rssparser.RssParserBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * One-time collector that rebuilds app/src/main/assets/classics.json from
 * historical Hacker News + the curated RSS feeds in topics.json. Original
 * titles are kept; summaries come from the RSS description or the page's
 * og:description / meta description (no rewriting). Items are keyword-
 * classified, URL-normalized, de-duplicated and self-checked (>= target,
 * unique ids/urls, non-blank, valid topic, http(s)) before the file is
 * written with a bumped poolVersion.
 *
 * Run from the repo root:
 *   ./gradlew :tools:classics-collector:run --args="--target 480"
 *
 * This tool is NOT shipped with the APK and is not run at runtime.
 */
fun main(args: Array<String>): Unit = runBlocking {
    val opts = parseArgs(args)
    val topicsWithFeeds = parseTopicsForCollector(File(opts.topicsPath).readText())
    val topics = topicsWithFeeds.map { it.first }
    val validTopicIds = topics.map { it.id }.toSet()

    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    val parser = RssParserBuilder().build()

    println("fetching RSS feeds…")
    val rss = fetchRssItems(parser, topicsWithFeeds)
    println("  ${rss.size} raw RSS items")

    println("fetching Hacker News best + top stories…")
    val hn = fetchHnItems(client, topics, hnLimit = 500)
    println("  ${hn.size} raw HN items")

    val cleaned = dedupeAndClean(rss + hn, validTopicIds).take(opts.target)
    val errors = selfCheck(cleaned, validTopicIds, minCount = opts.target)
    if (errors.isNotEmpty()) {
        System.err.println("self-check failed (${cleaned.size} items after clean, target ${opts.target}):")
        errors.forEach { System.err.println("  - $it") }
        exitProcess(1)
    }

    val existingVersion = runCatching { parseExistingPoolVersion(File(opts.outPath).readText()) }.getOrNull()
    val poolVersion = nextPoolVersion(existingVersion)
    File(opts.outPath).writeText(encodeClassics(poolVersion, cleaned))
    println("wrote ${cleaned.size} items (poolVersion=$poolVersion) to ${opts.outPath}")
}

private data class Options(
    val target: Int = 480,
    val topicsPath: String = "app/src/main/assets/topics.json",
    val outPath: String = "app/src/main/assets/classics.json",
)

private fun parseArgs(args: Array<String>): Options {
    var opts = Options()
    val i = args.iterator()
    while (i.hasNext()) {
        when (i.next()) {
            "--target" -> opts = opts.copy(target = i.next().toInt())
            "--topics" -> opts = opts.copy(topicsPath = i.next())
            "--out" -> opts = opts.copy(outPath = i.next())
        }
    }
    return opts
}

private suspend fun fetchRssItems(
    parser: com.prof18.rssparser.RssParser,
    topicsWithFeeds: List<Pair<CollectorTopic, List<String>>>,
): List<CollectorItem> = coroutineScope {
    val feeds = topicsWithFeeds.flatMap { (topic, urls) -> urls.map { topic to it } }
    feeds.map { (topic, feed) ->
        async {
            val channel = runCatching { parser.getRssChannel(feed) }.getOrNull() ?: return@async emptyList()
            val source = channel.title?.trim()?.takeUnless { it.isNullOrBlank() } ?: hostOf(feed)
            channel.items.mapNotNull { item ->
                val link = item.link?.trim()?.takeUnless { it.isNullOrBlank() } ?: return@mapNotNull null
                val title = item.title?.trim()?.takeUnless { it.isNullOrBlank() } ?: return@mapNotNull null
                val topicId = classifyTopic(title + " " + stripHtml(item.description), listOf(topic)) ?: topic.id
                CollectorItem(
                    title = title,
                    url = link,
                    summary = stripHtml(item.description),
                    topicId = topicId,
                    source = source,
                )
            }
        }
    }.awaitAll().flatten()
}

private suspend fun fetchHnStoryIds(client: OkHttpClient, endpoint: String): List<Long> =
    runCatching {
        val body = client.newCall(
            okhttp3.Request.Builder().url("https://hacker-news.firebaseio.com/v0/$endpoint").build(),
        ).execute().use { it.body?.string() }
        parseJson.decodeFromString<List<Long>>(body ?: "[]")
    }.getOrDefault(emptyList())

private suspend fun fetchHnItems(
    client: OkHttpClient,
    topics: List<CollectorTopic>,
    hnLimit: Int,
): List<CollectorItem> = coroutineScope {
    val ids: List<Long> = runCatching {
        // union best + top stories (heavy overlap) for a wider candidate set
        val best = fetchHnStoryIds(client, "beststories.json")
        val top = fetchHnStoryIds(client, "topstories.json")
        (best + top).distinct()
    }.getOrDefault(emptyList())

    ids.take(hnLimit).map { id ->
        async {
            val item = runCatching {
                val body = client.newCall(
                    okhttp3.Request.Builder().url("https://hacker-news.firebaseio.com/v0/item/$id.json").build(),
                ).execute().use { it.body?.string() }
                parseJson.decodeFromString<HnItem>(body ?: "{}")
            }.getOrNull() ?: return@async null

            if (item.type != "story") return@async null
            val title = item.title?.takeIf { it.isNotBlank() } ?: return@async null
            val url = item.url?.takeIf { it.isNotBlank() } ?: return@async null
            val topicId = classifyTopic(title, topics) ?: return@async null
            val summary = scrapeSummary(client, url)
            CollectorItem(title = title, url = url, summary = summary, topicId = topicId, source = "Hacker News")
        }
    }.awaitAll().filterNotNull()
}

private fun scrapeSummary(client: OkHttpClient, url: String): String {
    if (!url.startsWith("http://") && !url.startsWith("https://")) return ""
    return runCatching {
        client.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { resp ->
            val html = resp.body?.string() ?: return@runCatching ""
            val doc = Jsoup.parse(html)
            val og = doc.selectFirst("meta[property=og:description]")?.attr("content")
            val meta = doc.selectFirst("meta[name=description]")?.attr("content")
            stripHtml(og?.takeIf { it.isNotBlank() } ?: meta ?: "")
        }
    }.getOrDefault("")
}

private fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull().orEmpty()

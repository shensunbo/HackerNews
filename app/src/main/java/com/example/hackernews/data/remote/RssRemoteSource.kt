package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic
import com.prof18.rssparser.RssParser

// 注：方法/字段名对应 RssParser 6.x（getRssChannel/items/link/description/pubDate）。
// 若依赖版本不同导致编译错误，按该版本 API 调整字段名。
class RssRemoteSource(
    private val parser: RssParser = RssParser(),
) {
    suspend fun fetch(topics: List<Topic>, nowMillis: Long): List<Article> {
        val out = mutableListOf<Article>()
        for (topic in topics) {
            for (feed in topic.feeds) {
                val channel = runCatching { parser.getRssChannel(feed) }.getOrNull() ?: continue
                val source = channel.title?.trim().takeUnless { it.isNullOrBlank() } ?: hostOf(feed)
                for (item in channel.items) {
                    val link = item.link?.trim()
                    if (link.isNullOrBlank()) continue
                    out += Article(
                        id = articleIdFor(link),
                        title = item.title?.trim().takeUnless { it.isNullOrBlank() } ?: link,
                        url = link,
                        summary = stripHtml(item.description),
                        source = source,
                        topicIds = listOf(topic.id),
                        publishedAt = parseRssDateMillis(item.pubDate) ?: nowMillis,
                        score = null,
                    )
                }
            }
        }
        return out
    }
}

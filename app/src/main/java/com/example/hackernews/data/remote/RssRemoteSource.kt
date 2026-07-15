package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder

// 注：方法/字段名对应 RssParser 6.x（getRssChannel/items/link/description/pubDate）。
// 若依赖版本不同导致编译错误，按该版本 API 调整字段名。
class RssRemoteSource(
    private val parser: RssParser = RssParserBuilder().build(),
) : RemoteArticleSource {
    override suspend fun fetch(topics: List<Topic>, nowMillis: Long): RemoteFetchResult {
        val out = mutableListOf<Article>()
        var successfulRequests = 0
        var failedRequests = 0
        for (topic in topics) {
            for (feed in topic.feeds) {
                val channelResult = runCatching { parser.getRssChannel(feed) }
                if (channelResult.isFailure) {
                    failedRequests++
                    continue
                }
                successfulRequests++
                val channel = channelResult.getOrThrow()
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
        return RemoteFetchResult(out, successfulRequests, failedRequests)
    }
}

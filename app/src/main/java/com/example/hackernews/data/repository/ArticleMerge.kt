package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article

fun mergeArticles(list: List<Article>): List<Article> =
    list.groupBy { it.id }.map { (_, group) ->
        group.reduce { acc, x ->
            acc.copy(
                title = acc.title.ifBlank { x.title },
                summary = acc.summary.ifBlank { x.summary },
                topicIds = (acc.topicIds + x.topicIds).distinct(),
                publishedAt = maxOf(acc.publishedAt, x.publishedAt),
                score = listOfNotNull(acc.score, x.score).maxOrNull(),
            )
        }
    }

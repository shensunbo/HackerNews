package com.example.hackernews.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hackernews.domain.model.Article
import com.example.hackernews.ui.theme.TerminalColors
import com.example.hackernews.ui.util.relativeTime

@Composable
fun ArticleRow(
    article: Article,
    nowMillis: Long,
    onOpen: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    showTime: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            article.topicIds.firstOrNull()?.let {
                TopicTag(it)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = "· ${article.source}",
                color = TerminalColors.PrimaryDim,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = article.title,
            color = TerminalColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (article.summary.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = article.summary,
                color = TerminalColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showTime) {
                Text(
                    text = relativeTime(article.publishedAt, nowMillis),
                    color = TerminalColors.PrimaryDim,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            article.score?.let {
                if (showTime) Spacer(Modifier.width(8.dp))
                Text(
                    text = "▲$it",
                    color = TerminalColors.Accent,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            BookmarkStar(article.isBookmarked, onClick = onToggleBookmark)
        }
    }
    HorizontalDivider(color = TerminalColors.Border)
}

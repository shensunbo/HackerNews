package com.example.hackernews.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.theme.TerminalColors
import kotlinx.coroutines.delay

@Composable
fun TopicTag(id: String, modifier: Modifier = Modifier) {
    Text(
        text = "[$id]",
        color = TerminalColors.Primary,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier,
    )
}

@Composable
fun BookmarkStar(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.12f else 1f,
        animationSpec = tween(durationMillis = 130),
        label = "bookmarkScale",
    )
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (active) "取消收藏" else "收藏"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (active) "★" else "☆",
            color = if (active) TerminalColors.Primary else TerminalColors.PrimaryDim,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.scale(scale),
        )
    }
}

@Composable
fun BlinkingCursor() {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(600)
            visible = !visible
        }
    }
    Text(
        text = if (visible) " █" else "  ",
        color = TerminalColors.Primary,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.clearAndSetSemantics { },
    )
}

@Composable
fun BrailleSpinner(label: String, modifier: Modifier = Modifier) {
    val frames = "⠋⠙⠹⠸⠼⠴⠦⠧"
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            index = (index + 1) % frames.length
        }
    }
    Row(
        modifier = modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${frames[index]} $label",
            color = TerminalColors.PrimaryDim,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = TerminalColors.TextSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun StatusBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalColors.SurfaceElevated)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "! $message",
            color = TerminalColors.Accent,
            style = MaterialTheme.typography.labelMedium,
        )
        if (onRetry != null) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "[重试]",
                    color = TerminalColors.Primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

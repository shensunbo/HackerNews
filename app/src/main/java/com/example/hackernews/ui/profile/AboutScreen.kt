package com.example.hackernews.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.theme.TerminalColors

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> about", onBack = onBack)
        Text(
            text = "dev-news v1.0\n" +
                "data source: Hacker News + curated RSS\n" +
                "links out only; copyright belongs to the source sites.",
            color = TerminalColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

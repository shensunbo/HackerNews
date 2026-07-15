package com.example.hackernews.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.theme.TerminalColors

@Composable
fun ProfileScreen(
    onOpenBookmarks: () -> Unit,
    onOpenTopics: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> whoami")
        ProfileMenuRow("★  收藏", onOpenBookmarks)
        ProfileMenuRow("#  Topic 偏好", onOpenTopics)
        ProfileMenuRow("⇱  阅读方式", onOpenReading)
        ProfileMenuRow("?  关于", onOpenAbout)
    }
}

@Composable
private fun ProfileMenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = TerminalColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "›",
            color = TerminalColors.PrimaryDim,
            style = MaterialTheme.typography.titleMedium,
        )
    }
    HorizontalDivider(color = TerminalColors.Border)
}

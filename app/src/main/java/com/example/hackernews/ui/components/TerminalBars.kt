package com.example.hackernews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.theme.TerminalColors

@Composable
fun TerminalAppBar(
    command: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onBack)
                        .semantics { contentDescription = "back" }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "‹",
                        color = TerminalColors.Primary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            } else {
                Box(Modifier.padding(start = 16.dp))
            }
            Text(
                text = command,
                color = TerminalColors.Primary,
                style = MaterialTheme.typography.titleLarge,
            )
            BlinkingCursor()
            if (action != null) {
                Spacer(Modifier.weight(1f))
                action()
            }
        }
        HorizontalDivider(color = TerminalColors.Border)
    }
}

data class BottomNavItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun TerminalBottomBar(items: List<BottomNavItem>, modifier: Modifier = Modifier) {
    Column(modifier) {
        HorizontalDivider(color = TerminalColors.Border)
        Row(Modifier.fillMaxWidth().background(TerminalColors.Surface)) {
            items.forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Tab, onClick = item.onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.selected) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(TerminalColors.Primary),
                        )
                    }
                    Text(
                        text = item.label,
                        color = if (item.selected) {
                            TerminalColors.Primary
                        } else {
                            TerminalColors.PrimaryDim
                        },
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

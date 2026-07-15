package com.example.hackernews.ui.profile.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.hackernews.domain.model.ReadingMode
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.theme.TerminalColors
import com.example.hackernews.ui.util.appViewModel

@Composable
fun ReadingModeScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { ReadingModeViewModel(it.preferencesStore) }
    val mode by viewModel.mode.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> reading --mode", onBack = onBack)
        ReadingOption(
            title = "Custom Tabs",
            subtitle = "应用内浮层打开",
            selected = mode == ReadingMode.CUSTOM_TABS,
            onClick = { viewModel.set(ReadingMode.CUSTOM_TABS) },
        )
        ReadingOption(
            title = "外部浏览器",
            subtitle = "跳转 Chrome 等",
            selected = mode == ReadingMode.EXTERNAL_BROWSER,
            onClick = { viewModel.set(ReadingMode.EXTERNAL_BROWSER) },
        )
        Text(
            text = "· 设备无 Custom Tabs 时自动回退到外部浏览器",
            color = TerminalColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ReadingOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) "(•)" else "( )",
            color = TerminalColors.Primary,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = TerminalColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                color = TerminalColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
    HorizontalDivider(color = TerminalColors.Border)
}

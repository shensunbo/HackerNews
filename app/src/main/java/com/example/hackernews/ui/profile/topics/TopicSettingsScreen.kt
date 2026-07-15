package com.example.hackernews.ui.profile.topics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
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
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.components.WeightSlider
import com.example.hackernews.ui.theme.TerminalColors
import com.example.hackernews.ui.util.appViewModel

@Composable
fun TopicSettingsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel {
        TopicSettingsViewModel(it.feedRepository, it.preferencesStore)
    }
    val topics by viewModel.topics.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> topics --config", onBack = onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            items(topics, key = { it.id }) { topic ->
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topic.name,
                            color = TerminalColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .toggleable(
                                    value = topic.enabled,
                                    role = Role.Switch,
                                    onValueChange = { enabled ->
                                        viewModel.setEnabled(topic.id, enabled)
                                    },
                                )
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (topic.enabled) "[ on ]" else "[off]",
                                color = if (topic.enabled) {
                                    TerminalColors.Primary
                                } else {
                                    TerminalColors.PrimaryDim
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    WeightSlider(
                        value = topic.weight,
                        enabled = topic.enabled,
                        onValueChange = { weight ->
                            viewModel.setWeight(topic.id, weight)
                        },
                    )
                }
                HorizontalDivider(color = TerminalColors.Border)
            }
        }
    }
}

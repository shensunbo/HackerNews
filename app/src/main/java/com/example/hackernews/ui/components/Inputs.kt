package com.example.hackernews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.theme.TerminalColors
import com.example.hackernews.ui.util.asciiWeightBar
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "grep> …",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalColors.SurfaceElevated)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = TerminalColors.PrimaryDim,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = TerminalColors.TextPrimary,
                ),
                cursorBrush = SolidColor(TerminalColors.Primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun TopicChipRow(
    chips: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        TopicChip("全部", selectedId == null) { onSelect(null) }
        chips.forEach { (id, label) ->
            Spacer(Modifier.width(8.dp))
            TopicChip(label, selectedId == id) { onSelect(id) }
        }
    }
}

@Composable
private fun TopicChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    val background = if (selected) TerminalColors.Primary else TerminalColors.Surface
    val foreground = if (selected) TerminalColors.Bg else TerminalColors.PrimaryDim
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(background)
            .then(
                if (selected) Modifier else Modifier.border(1.dp, TerminalColors.Border, shape),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = foreground, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun WeightSlider(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayColor = if (enabled) TerminalColors.PrimaryDim else TerminalColors.Disabled
    Column(modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = if (enabled) {
                    "weight ${asciiWeightBar(value)}  ${String.format(Locale.US, "%.1f", value)}"
                } else {
                    "weight ░░░░░░░░  --"
                },
                color = displayColor,
                style = MaterialTheme.typography.labelMedium,
            )
            Slider(
                value = value.coerceIn(0f, 2f),
                onValueChange = {
                    onValueChange((it * 10f).roundToInt() / 10f)
                },
                valueRange = 0f..2f,
                steps = 19,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                ),
            )
        }
    }
}

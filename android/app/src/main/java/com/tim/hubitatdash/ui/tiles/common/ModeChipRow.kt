package com.tim.hubitatdash.ui.tiles.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.ui.theme.TileTokens

/**
 * A compact chip grid for mode / HSM selectors. Mirrors the web's
 * `grid-cols-3 gap-1.5` layout: selected chip gets a blue background,
 * the rest render as neutral gray.
 */
@Composable
fun ModeChipRow(
    options: List<String>,
    selectedLabel: String?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    selectedColor: Color = TileTokens.BlueCold
) {
    val rows = options.withIndex().chunked(columns)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowOptions.forEach { (idx, label) ->
                    Chip(
                        label = label,
                        selected = label.equals(selectedLabel, ignoreCase = true),
                        onClick = { onSelect(idx) },
                        selectedColor = selectedColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowOptions.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val bg = when {
        selected -> selectedColor
        dark -> TileTokens.PillOffBgDark
        else -> TileTokens.PillOffBgLight
    }
    val fg = when {
        selected -> Color.White
        dark -> TileTokens.PillOffTextDark
        else -> TileTokens.PillOffTextLight
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 32.dp)
            .clip(RoundedCornerShape(TileTokens.PillRadius))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}


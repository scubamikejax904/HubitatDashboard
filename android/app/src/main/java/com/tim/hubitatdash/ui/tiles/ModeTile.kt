package com.tim.hubitatdash.ui.tiles

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.data.model.HubMode
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.ModeChipRow
import com.tim.hubitatdash.ui.tiles.common.TileShell

@Composable
fun ModeTile(
    tile: TileConfig,
    modes: List<HubMode>,
    onSetMode: (modeId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMode = modes.firstOrNull { it.active }

    TileShell(
        title = tile.displayTitle,
        modifier = modifier,
        titleTrailing = {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = TileTokens.TitleMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = activeMode?.name ?: "—",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    ) {
        if (modes.isEmpty()) {
            Text("Loading…", style = MaterialTheme.typography.labelSmall, color = TileTokens.TitleMuted)
        } else {
            ModeChipRow(
                options = modes.map { it.name },
                selectedLabel = activeMode?.name,
                onSelect = { idx -> onSetMode(modes[idx].id) }
            )
        }
    }
}


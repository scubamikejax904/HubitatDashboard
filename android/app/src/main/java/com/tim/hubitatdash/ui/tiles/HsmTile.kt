package com.tim.hubitatdash.ui.tiles

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.data.model.HsmMode
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.ModeChipRow
import com.tim.hubitatdash.ui.tiles.common.TileShell

private fun hsmColor(mode: HsmMode): Color = when (mode) {
    HsmMode.ARMED_AWAY  -> TileTokens.RedAlert
    HsmMode.ARMED_HOME  -> TileTokens.OrangeHot
    HsmMode.ARMED_NIGHT -> TileTokens.BlueCold
    HsmMode.DISARMED, HsmMode.ALL_DISARMED -> TileTokens.GreenOn
    HsmMode.UNKNOWN     -> TileTokens.TitleMuted
}

private val hsmOptions = listOf(
    HsmMode.ARMED_AWAY,
    HsmMode.ARMED_HOME,
    HsmMode.ARMED_NIGHT,
    HsmMode.DISARMED
)

@Composable
fun HsmTile(
    tile: TileConfig,
    hsmStatus: HsmMode,
    onSetHsmMode: (mode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = hsmColor(hsmStatus)

    TileShell(
        title = tile.displayTitle,
        modifier = modifier,
        titleTrailing = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = hsmStatus.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    ) {
        ModeChipRow(
            options = hsmOptions.map { it.displayName },
            selectedLabel = hsmStatus.displayName,
            onSelect = { idx -> onSetHsmMode(hsmOptions[idx].apiValue) },
            selectedColor = color
        )
    }
}


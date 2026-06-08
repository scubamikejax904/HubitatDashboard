package com.tim.hubitatdash.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.HubVariable
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.TilePillSkeleton
import com.tim.hubitatdash.ui.tiles.common.TileShell
import com.tim.hubitatdash.ui.tiles.common.TileStatusChip

@Composable
fun RingDetectionTile(
    tile: TileConfig,
    device: DeviceState?,
    hubVariables: List<HubVariable>,
    modifier: Modifier = Modifier
) {
    val isActive = device?.attributes?.get("motion") == "active"
    val color = if (isActive) TileTokens.RedAlert else TileTokens.TitleMuted
    val varValue = tile.hubVarName?.let { name ->
        hubVariables.firstOrNull { it.name == name }?.value
    }
    TileShell(title = tile.displayTitle, modifier = modifier) {
        TileStatusChip(
            text = varValue ?: if (isActive) "Active" else "Inactive",
            color = color,
            icon = if (isActive) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone
        )
    }
}


package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.HubVariable
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.theme.TileTokens
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePillSkeleton
import com.timshubet.hubitatdashboard.ui.tiles.common.TileShell
import com.timshubet.hubitatdashboard.ui.tiles.common.TileStatusChip

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

package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.theme.TileTokens
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePillSkeleton
import com.timshubet.hubitatdashboard.ui.tiles.common.TileShell
import com.timshubet.hubitatdashboard.ui.tiles.common.TileStatusChip

@Composable
fun MotionTile(tile: TileConfig, device: DeviceState?, modifier: Modifier = Modifier) {
    val isActive = device?.attributes?.get("motion") == "active"
    val color = if (isActive) TileTokens.AmberActive else TileTokens.TitleMuted
    TileShell(title = tile.displayTitle, modifier = modifier) {
        TileStatusChip(
            text = if (isActive) "Active" else "Inactive",
            color = color,
            icon = if (isActive) Icons.Filled.DirectionsRun else Icons.Filled.Person
        )
    }
}

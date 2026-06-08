package com.tim.hubitatdash.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.TilePillSkeleton
import com.tim.hubitatdash.ui.tiles.common.TileShell
import com.tim.hubitatdash.ui.tiles.common.TileStatusChip

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


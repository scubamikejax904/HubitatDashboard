package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.theme.TileTokens
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePillSkeleton
import com.timshubet.hubitatdashboard.ui.tiles.common.TileShell
import com.timshubet.hubitatdashboard.ui.tiles.common.TileStatusChip

@Composable
fun ContactTile(tile: TileConfig, device: DeviceState?, modifier: Modifier = Modifier) {
    val isOpen = device?.attributes?.get("contact") == "open"
    val color = if (isOpen) TileTokens.OrangeHot else TileTokens.GreenOn
    TileShell(title = tile.displayTitle, modifier = modifier) {
        TileStatusChip(
            text = if (isOpen) "Open" else "Closed",
            color = color,
            icon = if (isOpen) Icons.Filled.DoorFront else Icons.Filled.DoorBack
        )
    }
}

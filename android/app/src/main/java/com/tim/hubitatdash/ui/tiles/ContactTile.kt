package com.tim.hubitatdash.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.TilePillSkeleton
import com.tim.hubitatdash.ui.tiles.common.TileShell
import com.tim.hubitatdash.ui.tiles.common.TileStatusChip

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


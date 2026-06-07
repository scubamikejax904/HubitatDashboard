package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.theme.TileTokens
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePillSkeleton
import com.timshubet.hubitatdashboard.ui.tiles.common.TileShell
import com.timshubet.hubitatdashboard.ui.tiles.common.TileStatusChip

@Composable
fun PresenceTile(tile: TileConfig, device: DeviceState?, modifier: Modifier = Modifier) {
    val isPresent = device?.attributes?.get("presence") == "present"
    val color = if (isPresent) TileTokens.GreenOn else TileTokens.TitleMuted
    TileShell(title = tile.displayTitle, modifier = modifier) {
        TileStatusChip(
            text = if (isPresent) "Present" else "Away",
            color = color,
            icon = if (isPresent) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle
        )
    }
}

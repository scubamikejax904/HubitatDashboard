package com.tim.hubitatdash.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.TilePillSkeleton
import com.tim.hubitatdash.ui.tiles.common.TileShell
import com.tim.hubitatdash.ui.tiles.common.TileValue

private fun batteryColor(level: Int): Color = when {
    level >= 50 -> TileTokens.GreenOn
    level >= 20 -> TileTokens.OrangeHot
    else        -> TileTokens.RedAlert
}

private fun batteryIcon(level: Int) = when {
    level >= 90 -> Icons.Filled.BatteryFull
    level >= 60 -> Icons.Filled.Battery6Bar
    level >= 40 -> Icons.Filled.Battery4Bar
    level >= 15 -> Icons.Filled.Battery2Bar
    else        -> Icons.Filled.Battery0Bar
}

@Composable
fun BatteryTile(tile: TileConfig, device: DeviceState?, modifier: Modifier = Modifier) {
    val batteryStr = device?.attributes?.get("battery")
    val level = batteryStr?.toFloatOrNull()?.toInt() ?: -1
    val color = if (level >= 0) batteryColor(level) else TileTokens.TitleMuted

    TileShell(title = tile.displayTitle, modifier = modifier) {
        TileValue(
            icon = if (level >= 0) batteryIcon(level) else Icons.Filled.Battery0Bar,
            value = if (level >= 0) "$level" else "—",
            unit = if (level >= 0) "%" else null,
            color = color
        )
    }
}


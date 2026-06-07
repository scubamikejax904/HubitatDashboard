package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.theme.TileTokens
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePillSkeleton
import com.timshubet.hubitatdashboard.ui.tiles.common.TileShell
import com.timshubet.hubitatdashboard.ui.tiles.common.TileValue

private fun temperatureColor(t: Float?): Color = when {
    t == null -> TileTokens.TitleMuted
    t < 65f   -> TileTokens.BlueCold
    t <= 80f  -> TileTokens.GreenComfort
    else      -> TileTokens.OrangeHot
}

@Composable
fun TemperatureTile(tile: TileConfig, device: DeviceState?, modifier: Modifier = Modifier) {
    val tempStr = device?.attributes?.get("temperature")
    val temp = tempStr?.toFloatOrNull()
    val humidity = device?.attributes?.get("humidity")
    val color = temperatureColor(temp)

    TileShell(title = tile.displayTitle, modifier = modifier) {
        TileValue(
            icon = Icons.Filled.Thermostat,
            value = tempStr ?: "—",
            unit = if (tempStr != null) "°F" else null,
            color = color
        )
        if (humidity != null) {
            Text(
                text = "$humidity% humidity",
                style = MaterialTheme.typography.labelSmall,
                color = TileTokens.TitleMuted
            )
        }
    }
}

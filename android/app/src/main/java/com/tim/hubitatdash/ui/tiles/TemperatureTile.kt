package com.tim.hubitatdash.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.TilePillSkeleton
import com.tim.hubitatdash.ui.tiles.common.TileShell
import com.tim.hubitatdash.ui.tiles.common.TileValue

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


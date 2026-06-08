package com.tim.hubitatdash.ui.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.TilePill
import com.tim.hubitatdash.ui.tiles.common.TilePillSkeleton
import com.tim.hubitatdash.ui.tiles.common.TileShell
import kotlinx.coroutines.launch

@Composable
fun DimmerTile(
    tile: TileConfig,
    device: DeviceState?,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceId = tile.deviceId ?: return
    val isOn = device?.attributes?.get("switch") == "on"
    val level = device?.attributes?.get("level")?.toFloatOrNull() ?: 0f
    var sliderValue by remember(level) { mutableFloatStateOf(level) }
    var isPending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val toggle: () -> Unit = {
        if (!isPending && device != null) {
            isPending = true
            scope.launch {
                onCommand(deviceId, if (isOn) "off" else "on", null)
                isPending = false
            }
        }
    }

    TileShell(
        title = tile.displayTitle,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TilePill(
                label = if (isOn) "On" else "Off",
                isOn = isOn,
                icon = Icons.Filled.BrightnessHigh,
                onClick = toggle,
                pending = isPending,
                onColor = TileTokens.AmberActive
            )
            Text(
                text = "${sliderValue.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (isOn) TileTokens.AmberActive else TileTokens.TitleMuted
            )
        }
        Slider(
            value = sliderValue / 100f,
            onValueChange = { sliderValue = it * 100f },
            onValueChangeFinished = {
                scope.launch {
                    onCommand(deviceId, "setLevel", sliderValue.toInt().toString())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}


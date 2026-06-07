package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.theme.TileTokens
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePill
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePillSkeleton
import com.timshubet.hubitatdashboard.ui.tiles.common.TileShell
import com.timshubet.hubitatdashboard.ui.tiles.common.TileValue
import kotlinx.coroutines.launch

@Composable
fun PowerMeterTile(
    tile: TileConfig,
    device: DeviceState?,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceId = tile.deviceId
    val powerStr = device?.attributes?.get("power")
    val powerVal = powerStr?.toFloatOrNull() ?: 0f
    val energy = device?.attributes?.get("energy")
    val hasSwitch = device?.attributes?.containsKey("switch") == true
    val isOn = device?.attributes?.get("switch") == "on"
    val isActive = isOn || (!hasSwitch && powerVal > 0f)
    val color = if (isActive) TileTokens.AmberActive else TileTokens.TitleMuted
    var isPending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    TileShell(title = tile.displayTitle, modifier = modifier) {
        TileValue(
            icon = Icons.Filled.ElectricBolt,
            value = powerStr ?: "—",
            unit = if (powerStr != null) "W" else null,
            color = color
        )
        if (energy != null) {
            Text(
                text = "$energy kWh",
                style = MaterialTheme.typography.labelSmall,
                color = TileTokens.TitleMuted
            )
        }
        if (hasSwitch && deviceId != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TilePill(
                    label = if (isOn) "On" else "Off",
                    isOn = isOn,
                    icon = Icons.Filled.ElectricBolt,
                    pending = isPending,
                    onColor = TileTokens.AmberActive,
                    onClick = {
                        if (!isPending) {
                            isPending = true
                            scope.launch {
                                onCommand(deviceId, if (isOn) "off" else "on", null)
                                isPending = false
                            }
                        }
                    }
                )
            }
        }
    }
}

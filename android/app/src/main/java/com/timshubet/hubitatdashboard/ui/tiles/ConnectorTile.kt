package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePill
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePillSkeleton
import com.timshubet.hubitatdashboard.ui.tiles.common.TileShell
import kotlinx.coroutines.launch

@Composable
fun ConnectorTile(
    tile: TileConfig,
    device: DeviceState?,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceId = tile.deviceId ?: return
    val isOn = device?.attributes?.get("switch") == "on"
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
        modifier = modifier.clickable(enabled = !isPending && device != null) { toggle() }
    ) {
        TilePill(
            label = if (isOn) "On" else "Off",
            isOn = isOn,
            icon = Icons.Filled.ToggleOn,
            onClick = toggle,
            pending = isPending
        )
    }
}

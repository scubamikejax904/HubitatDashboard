package com.tim.hubitatdash.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.TilePill
import com.tim.hubitatdash.ui.tiles.common.TileShell
import kotlinx.coroutines.launch

@Composable
fun ButtonTile(
    tile: TileConfig,
    device: DeviceState?,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceId = tile.deviceId ?: return
    var isPending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    TileShell(title = tile.displayTitle, modifier = modifier) {
        TilePill(
            label = "Push",
            isOn = true, // button is always emphasized as actionable
            icon = Icons.Filled.TouchApp,
            pending = isPending,
            onColor = TileTokens.BlueCold,
            onClick = {
                if (!isPending) {
                    isPending = true
                    scope.launch {
                        onCommand(deviceId, "push", "1")
                        isPending = false
                    }
                }
            }
        )
    }
}


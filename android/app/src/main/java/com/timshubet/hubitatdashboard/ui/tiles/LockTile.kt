package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.theme.TileTokens
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePill
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePillSkeleton
import com.timshubet.hubitatdashboard.ui.tiles.common.TileShell

@Composable
fun LockTile(
    tile: TileConfig,
    device: DeviceState?,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceId = tile.deviceId ?: return
    val isLocked = device?.attributes?.get("lock") == "locked"

    TileShell(title = tile.displayTitle, modifier = modifier) {
        TilePill(
            label = if (isLocked) "Locked" else "Unlocked",
            isOn = isLocked,
            icon = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
            onClick = { onCommand(deviceId, if (isLocked) "unlock" else "lock", null) },
            onColor = if (isLocked) TileTokens.GreenOn else TileTokens.RedAlert
        )
    }
}

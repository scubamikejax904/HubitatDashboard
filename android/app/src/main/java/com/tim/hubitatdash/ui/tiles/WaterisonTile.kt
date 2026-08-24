package com.tim.hubitatdash.ui.tiles

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Water
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.HubVariable
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.tiles.common.TilePill
import com.tim.hubitatdash.ui.tiles.common.TileShell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Special water-status tile: switch behavior with a live auto-off countdown.
 * The countdown anchors to when the tile first observes the switch ON (per
 * "on" period), and survives recomposition while the composable stays alive.
 * Mirrors the web WaterisonTile: shows remaining time until the hub's
 * hubVariable-based auto-off fires.
 */
@Composable
fun WaterisonTile(
    tile: TileConfig,
    device: DeviceState?,
    hubVariables: List<HubVariable>,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceId = tile.deviceId ?: return
    val isOn = device?.attributes?.get("switch") == "on"
    var isPending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Resolve the tile's hub variable value (minutes until auto-off)
    val hubVarName = tile.hubVarName
    val minutes = hubVariables.firstOrNull { it.name == hubVarName }?.value?.toDoubleOrNull()

    // Anchor countdown start once per "on" period; reset when turned off
    var startedAtMs by remember(hubVarName) { mutableLongStateOf(0L) }
    LaunchedEffect(isOn, minutes) {
        if (!isOn || minutes == null) {
            startedAtMs = 0L
        } else if (startedAtMs == 0L) {
            startedAtMs = System.currentTimeMillis()
        }
    }

    // Tick every second while counting down
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMs, isOn) {
        if (startedAtMs != 0L && isOn) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    val totalSeconds = ((minutes ?: 0.0) * 60).toLong()
    val secondsLeft = if (isOn && startedAtMs != 0L && totalSeconds > 0)
        ((totalSeconds - (nowMs - startedAtMs) / 1000).coerceAtLeast(0))
    else null

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
        if (secondsLeft != null) {
            val m = secondsLeft / 60
            val s = secondsLeft % 60
            TilePill(
                label = "%d:%02d".format(m, s),
                isOn = true,
                icon = Icons.Filled.Water,
                onClick = toggle,
                pending = isPending
            )
        } else {
            TilePill(
                label = if (isOn) "On" else "Off",
                isOn = isOn,
                icon = Icons.Filled.ToggleOn,
                onClick = toggle,
                pending = isPending
            )
        }
    }
}

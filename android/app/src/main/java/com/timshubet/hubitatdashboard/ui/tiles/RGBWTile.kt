package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.theme.TileTokens
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePill
import com.timshubet.hubitatdashboard.ui.tiles.common.TilePillSkeleton
import com.timshubet.hubitatdashboard.ui.tiles.common.TileShell
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RGBWTile(
    tile: TileConfig,
    device: DeviceState?,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceId = tile.deviceId ?: return
    val isOn = device?.attributes?.get("switch") == "on"
    val hue = device?.attributes?.get("hue")?.toFloatOrNull() ?: 0f
    val saturation = device?.attributes?.get("saturation")?.toFloatOrNull() ?: 100f
    val level = device?.attributes?.get("level")?.toFloatOrNull() ?: 100f
    val hue360 = hue * 3.6f
    val swatchColor = if (isOn) hsvToColor(hue360, saturation / 100f, level / 100f)
                      else Color(0xFF424242)

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    var editHue by remember(hue360) { mutableFloatStateOf(hue360) }
    var editSat by remember(saturation) { mutableFloatStateOf(saturation) }
    var editLevel by remember(level) { mutableFloatStateOf(level) }

    TileShell(title = tile.displayTitle, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TilePill(
                label = if (isOn) "On" else "Off",
                isOn = isOn,
                icon = Icons.Filled.Palette,
                onClick = {
                    scope.launch { onCommand(deviceId, if (isOn) "off" else "on", null) }
                },
                onColor = TileTokens.PurpleRgb
            )
            // Current color swatch — tap to open picker
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .clickable { showSheet = true }
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tile.label, style = MaterialTheme.typography.titleMedium)

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(hsvToColor(editHue, editSat / 100f, editLevel / 100f))
                        .align(Alignment.CenterHorizontally)
                )

                Text("Hue: ${editHue.toInt()}°", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = editHue / 360f,
                    onValueChange = { editHue = it * 360f },
                    onValueChangeFinished = { sendColor(deviceId, editHue, editSat, editLevel, onCommand) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Saturation: ${editSat.toInt()}%", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = editSat / 100f,
                    onValueChange = { editSat = it * 100f },
                    onValueChangeFinished = { sendColor(deviceId, editHue, editSat, editLevel, onCommand) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Brightness: ${editLevel.toInt()}%", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = editLevel / 100f,
                    onValueChange = { editLevel = it * 100f },
                    onValueChangeFinished = {
                        onCommand(deviceId, "setLevel", editLevel.toInt().toString())
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { onCommand(deviceId, "off", null) }) { Text("Off") }
                    TextButton(onClick = { onCommand(deviceId, "on", null) }) { Text("On") }
                }
            }
        }
    }
}

private fun sendColor(
    deviceId: String,
    hue360: Float,
    saturation: Float,
    level: Float,
    onCommand: (String, String, String?) -> Unit
) {
    val hubitatHue = (hue360 / 3.6f).toInt()
    val colorMap = "[hue:$hubitatHue, saturation:${saturation.toInt()}, level:${level.toInt()}]"
    onCommand(deviceId, "setColor", colorMap)
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val argb = ColorUtils.HSLToColor(floatArrayOf(hue, saturation, value * 0.5f + 0.1f))
    return Color(argb)
}

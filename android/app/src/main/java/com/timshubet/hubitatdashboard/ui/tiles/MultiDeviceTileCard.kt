package com.timshubet.hubitatdashboard.ui.tiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.MultiTileConfig
import com.timshubet.hubitatdashboard.data.model.TileConfig
import com.timshubet.hubitatdashboard.ui.theme.TileTokens

private fun tempColor(temp: Float?): Color = when {
    temp == null -> Color.White
    temp < 65f   -> TileTokens.BlueCold
    temp <= 80f  -> TileTokens.GreenComfort
    else         -> TileTokens.OrangeHot
}

@Composable
fun MultiDeviceTileCard(
    tile: TileConfig,
    config: MultiTileConfig?,
    devices: Map<String, DeviceState>,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (config == null || config.deviceIds.isEmpty()) return

    Log.d("MultiTileCard", "tile=${tile.deviceId} labels=${config.labels} deviceIds=${config.deviceIds}")
    val cols = config.cols.coerceIn(1, 4)
    val label = config.label?.ifBlank { null } ?: "Panel"
    val deviceIds = config.deviceIds.filter { devices.containsKey(it) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, TileTokens.MultiPanelBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val rows = deviceIds.chunked(cols)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { rowDevices ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowDevices.forEach { deviceId ->
                            val cellLabel = config.labels?.get(deviceId)?.takeIf { it.isNotBlank() }
                                ?: devices[deviceId]!!.label
                            MiniDeviceCell(
                                deviceId = deviceId,
                                device = devices[deviceId]!!,
                                displayLabel = cellLabel,
                                onCommand = onCommand,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(cols - rowDevices.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniDeviceCell(
    deviceId: String,
    device: DeviceState,
    displayLabel: String,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val attrs = device.attributes
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = displayLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )

            when {
                attrs.containsKey("switch") && attrs.containsKey("level") -> {
                    // Dimmer
                    val isOn = attrs["switch"] == "on"
                    val level = attrs["level"]?.toString()
                    val btnLabel = if (isOn && level != null) "$level%" else if (isOn) "On" else "Off"
                    if (isOn) {
                        Button(
                            onClick = { onCommand(deviceId, "off", null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TileTokens.AmberActive,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(btnLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onCommand(deviceId, "on", null) },
                            border = BorderStroke(1.5.dp, TileTokens.OffBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(btnLabel, fontSize = 12.sp)
                        }
                    }
                }
                attrs.containsKey("switch") -> {
                    // Switch
                    val isOn = attrs["switch"] == "on"
                    if (isOn) {
                        Button(
                            onClick = { onCommand(deviceId, "off", null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TileTokens.GreenOn,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("On", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onCommand(deviceId, "on", null) },
                            border = BorderStroke(1.5.dp, TileTokens.OffBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Off", fontSize = 12.sp)
                        }
                    }
                }
                attrs.containsKey("temperature") -> {
                    val t = attrs["temperature"]?.toFloatOrNull()
                    Text(
                        text = if (t != null) "$t°" else "—",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tempColor(t)
                    )
                }
                attrs.containsKey("contact") -> {
                    val isOpen = attrs["contact"] == "open"
                    Text(
                        text = if (isOpen) "Open" else "Closed",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOpen) TileTokens.OrangeHot else TileTokens.GreenOn
                    )
                }
                attrs.containsKey("motion") -> {
                    val active = attrs["motion"] == "active"
                    Text(
                        text = if (active) "Active" else "Clear",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) TileTokens.AmberActive else TileTokens.TitleMuted
                    )
                }
                attrs.containsKey("presence") -> {
                    val present = attrs["presence"] == "present"
                    Text(
                        text = if (present) "Home" else "Away",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (present) TileTokens.GreenOn else TileTokens.TitleMuted
                    )
                }
                else -> {
                    Text(
                        text = attrs.values.firstOrNull()?.toString() ?: "—",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

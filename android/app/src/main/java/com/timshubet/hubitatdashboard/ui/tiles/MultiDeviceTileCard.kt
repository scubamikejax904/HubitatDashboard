package com.timshubet.hubitatdashboard.ui.tiles

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
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.MultiTileConfig
import com.timshubet.hubitatdashboard.data.model.TileConfig

private val White = Color.White

@Composable
fun MultiDeviceTileCard(
    tile: TileConfig,
    config: MultiTileConfig?,
    devices: Map<String, DeviceState>,
    onCommand: (deviceId: String, command: String, value: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (config == null || config.deviceIds.isEmpty()) return

    val cols = config.cols.coerceIn(1, 4)
    val label = config.label?.ifBlank { null } ?: "Panel"
    val deviceIds = config.deviceIds.filter { devices.containsKey(it) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = White,
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
                            MiniDeviceCell(
                                deviceId = deviceId,
                                device = devices[deviceId]!!,
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
                text = device.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = White
            )

            when {
                attrs.containsKey("switch") && attrs.containsKey("level") -> {
                    val isOn = attrs["switch"] == "on"
                    val level = attrs["level"]?.toString()
                    val btnLabel = if (isOn && level != null) "$level%" else if (isOn) "On" else "Off"
                    Button(
                        onClick = { onCommand(deviceId, if (isOn) "off" else "on", null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOn) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(btnLabel, fontSize = 12.sp, color = White)
                    }
                }
                attrs.containsKey("switch") -> {
                    val isOn = attrs["switch"] == "on"
                    Button(
                        onClick = { onCommand(deviceId, if (isOn) "off" else "on", null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOn) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(if (isOn) "On" else "Off", fontSize = 12.sp, color = White)
                    }
                }
                attrs.containsKey("temperature") -> {
                    Text(
                        text = attrs["temperature"]?.let { "$it" + "°" } ?: "—",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                }
                attrs.containsKey("contact") -> {
                    val isOpen = attrs["contact"] == "open"
                    Text(
                        text = attrs["contact"]?.let { if (isOpen) "Open" else "Closed" } ?: "—",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                }
                attrs.containsKey("motion") -> {
                    val active = attrs["motion"] == "active"
                    Text(
                        text = attrs["motion"]?.let { if (active) "Active" else "Clear" } ?: "—",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                }
                attrs.containsKey("presence") -> {
                    val present = attrs["presence"] == "present"
                    Text(
                        text = attrs["presence"]?.let { if (present) "Home" else "Away" } ?: "—",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                }
                else -> {
                    Text(
                        text = attrs.values.firstOrNull()?.toString() ?: "—",
                        fontSize = 13.sp,
                        color = White
                    )
                }
            }
        }
    }
}

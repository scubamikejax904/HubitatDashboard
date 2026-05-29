package com.timshubet.hubitatdashboard.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.timshubet.hubitatdashboard.data.model.ConnectionStatus
import com.timshubet.hubitatdashboard.data.model.ConnectionType
import com.timshubet.hubitatdashboard.data.model.HsmMode
import com.timshubet.hubitatdashboard.viewmodel.DeviceViewModel
import com.timshubet.hubitatdashboard.viewmodel.HubitatNotificationViewModel
import com.timshubet.hubitatdashboard.viewmodel.RingListenerViewModel

// Connector switches shown in the system status bar
private data class ConnectorChipConfig(val deviceId: String, val label: String)

private val connectorChips = listOf(
    ConnectorChipConfig("486", "Alarms"),
    ConnectorChipConfig("905", "Silent"),
    ConnectorChipConfig("1227", "High Alert"),
    ConnectorChipConfig("1268", "Traveling"),
    ConnectorChipConfig("1327", "PTO"),
    ConnectorChipConfig("1316", "Holiday")
)

// Color helpers
private val ColorArmedAway = Color(0xFFD32F2F)    // red
private val ColorArmedHome = Color(0xFFF57C00)    // amber
private val ColorArmedNight = Color(0xFF1565C0)   // blue
private val ColorDisarmed = Color(0xFF2E7D32)     // green
private val ColorConnected = Color(0xFF2E7D32)    // green
private val ColorReconnecting = Color(0xFFF57C00) // amber
private val ColorOn = Color(0xFFF57C00)           // amber
private val ColorOff = Color(0xFF757575)          // gray

private fun hsmColor(mode: HsmMode): Color = when (mode) {
    HsmMode.ARMED_AWAY -> ColorArmedAway
    HsmMode.ARMED_HOME -> ColorArmedHome
    HsmMode.ARMED_NIGHT -> ColorArmedNight
    HsmMode.DISARMED, HsmMode.ALL_DISARMED -> ColorDisarmed
    HsmMode.UNKNOWN -> ColorOff
}

@Composable
fun SystemStatusRow(
    viewModel: DeviceViewModel,
    modifier: Modifier = Modifier,
    ringListenerViewModel: RingListenerViewModel? = null,
    onRingListenerClick: (() -> Unit)? = null,
    hubitatNotificationViewModel: HubitatNotificationViewModel? = null,
    onHubitatListenerClick: (() -> Unit)? = null,
    onLogsClick: (() -> Unit)? = null
) {
    val hsmStatus by viewModel.hsmStatus.collectAsState()
    val modes by viewModel.modes.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val activeConnection by viewModel.activeConnection.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()

    val activeMode = modes.firstOrNull { it.active }

    val ringPermissionGranted = ringListenerViewModel?.permissionGranted?.collectAsState()?.value
    val ringServiceConnected = ringListenerViewModel?.serviceConnected?.collectAsState()?.value

    val hubitatPermissionGranted = hubitatNotificationViewModel?.permissionGranted?.collectAsState()?.value
    val hubitatServiceConnected = hubitatNotificationViewModel?.serviceConnected?.collectAsState()?.value

    Surface(
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Ring Listener chip (first)
            if (ringListenerViewModel != null && onRingListenerClick != null) {
                item {
                    val (ringIcon, ringLabel, ringColor) = when {
                        ringPermissionGranted == true && ringServiceConnected == true ->
                            Triple(Icons.Default.NotificationsActive, "Ring ●", ColorConnected)
                        ringPermissionGranted == true ->
                            Triple(Icons.Default.NotificationsActive, "Ring ○", ColorReconnecting)
                        else ->
                            Triple(Icons.Default.NotificationsOff, "Ring !", ColorArmedAway)
                    }
                    AssistChip(
                        onClick = onRingListenerClick,
                        label = { Text(ringLabel, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(ringIcon, contentDescription = "Ring Listener", tint = ringColor)
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ringColor.copy(alpha = 0.12f)
                        )
                    )
                }
            }

            // Logs chip (after Ring)
            if (onLogsClick != null) {
                item {
                    AssistChip(
                        onClick = onLogsClick,
                        label = { Text("Logs", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "All Notifications Log", tint = ColorOff)
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ColorOff.copy(alpha = 0.12f)
                        )
                    )
                }
            }

            // Hubitat Notification chip (second)
            if (hubitatNotificationViewModel != null && onHubitatListenerClick != null) {
                item {
                    val (hubitatIcon, hubitatLabel, hubitatColor) = when {
                        hubitatPermissionGranted == true && hubitatServiceConnected == true ->
                            Triple(Icons.Default.NotificationsActive, "Hubitat ●", ColorConnected)
                        hubitatPermissionGranted == true ->
                            Triple(Icons.Default.NotificationsActive, "Hubitat ○", ColorReconnecting)
                        else ->
                            Triple(Icons.Default.NotificationsOff, "Hubitat !", ColorArmedAway)
                    }
                    AssistChip(
                        onClick = onHubitatListenerClick,
                        label = { Text(hubitatLabel, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(hubitatIcon, contentDescription = "Hubitat Notifications", tint = hubitatColor)
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = hubitatColor.copy(alpha = 0.12f)
                        )
                    )
                }
            }

            // HSM chip
            item {
                val hsmColor = hsmColor(hsmStatus)
                AssistChip(
                    onClick = {},
                    label = { Text(hsmStatus.displayName, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "HSM",
                            tint = hsmColor
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = hsmColor.copy(alpha = 0.12f)
                    )
                )
            }

            // Mode chip
            item {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            activeMode?.name ?: "—",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = "Mode", tint = Color(0xFF1565C0))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF1565C0).copy(alpha = 0.12f)
                    )
                )
            }

            // Connection chip
            item {
                val (connIcon, connLabel, connColor) = when {
                    connectionStatus == ConnectionStatus.RECONNECTING ->
                        Triple(Icons.Default.SyncProblem, "Reconnecting${connectionError?.let { ": $it" } ?: ""}", ColorReconnecting)
                    activeConnection == ConnectionType.LOCAL ->
                        Triple(Icons.Default.Wifi, "Local", ColorConnected)
                    activeConnection == ConnectionType.CLOUD ->
                        Triple(Icons.Default.Cloud, "Cloud", ColorConnected)
                    else -> Triple(Icons.Default.WifiOff, "Unknown", ColorOff)
                }
                AssistChip(
                    onClick = {},
                    label = { Text(connLabel, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(connIcon, contentDescription = "Connection", tint = connColor) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = connColor.copy(alpha = 0.12f)
                    )
                )
            }

            // Connector switch chips
            items(connectorChips) { config ->
                val device = devices[config.deviceId]
                val isOn = device?.attributes?.get("switch") == "on"
                val chipColor = if (isOn) ColorOn else ColorOff
                AssistChip(
                    onClick = {},
                    label = { Text(config.label, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(Icons.Default.Router, contentDescription = config.label, tint = chipColor)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = chipColor.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}

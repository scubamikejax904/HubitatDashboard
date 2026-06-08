package com.tim.hubitatdash.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.TileType

private fun TileType.displayName(): String = when (this) {
    TileType.SWITCH      -> "Switch (On/Off)"
    TileType.DIMMER      -> "Dimmer (Level)"
    TileType.RGBW        -> "Color Light (RGBW)"
    TileType.CONNECTOR   -> "Connector (On/Off)"
    TileType.LOCK        -> "Lock"
    TileType.CONTACT     -> "Contact Sensor"
    TileType.MOTION      -> "Motion Sensor"
    TileType.PRESENCE    -> "Presence"
    TileType.TEMPERATURE -> "Temperature"
    TileType.POWER_METER -> "Power Meter"
    TileType.BUTTON      -> "Button"
    TileType.BATTERY     -> "Battery"
    TileType.RING_DETECTION -> "Ring Detection"
    TileType.HUB_VARIABLE -> "Hub Variable"
    TileType.HSM         -> "Security System"
    TileType.MODE        -> "Hub Mode"
    TileType.SUN_TIMES   -> "Sun Times (web only)"
    TileType.MULTI_DEVICE -> "Multi-Device (web only)"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileTypePickerSheet(
    device: DeviceState,
    currentType: TileType,
    onDismiss: () -> Unit,
    onSelect: (TileType) -> Unit
) {
    val available = availableTileTypes(device)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Display as…",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = device.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            LazyColumn {
                items(available) { type ->
                    val isSelected = type == currentType
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(type)
                                onDismiss()
                            }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.displayName(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


package com.tim.hubitatdash.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.TileType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePickerSheet(
    groupId: String,
    currentDeviceIds: Set<String>,
    devices: Map<String, DeviceState>,
    onDismiss: () -> Unit,
    onAdd: (deviceId: String, label: String, tileType: TileType) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Add Device", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            val filteredDevices = devices.values
                .sortedBy { it.label }
                .filter { device ->
                    device.id !in currentDeviceIds &&
                    (searchQuery.isBlank() ||
                     device.label.contains(searchQuery, ignoreCase = true) ||
                     device.type.contains(searchQuery, ignoreCase = true))
                }

            val showSpecial = searchQuery.isBlank()

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                if (showSpecial) {
                    item {
                        Text(
                            "Special Tiles",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if ("__hsm__" !in currentDeviceIds) {
                        item {
                            SpecialTileRow("Security System", TileType.HSM) {
                                onAdd("__hsm__", "Security System", TileType.HSM)
                                onDismiss()
                            }
                        }
                    }
                    if ("__mode__" !in currentDeviceIds) {
                        item {
                            SpecialTileRow("Hub Mode", TileType.MODE) {
                                onAdd("__mode__", "Hub Mode", TileType.MODE)
                                onDismiss()
                            }
                        }
                    }
                    if ("__sunrise__" !in currentDeviceIds) {
                        item {
                            SpecialTileRow("Sunrise", TileType.HUB_VARIABLE) {
                                onAdd("__sunrise__", "Sunrise", TileType.HUB_VARIABLE)
                                onDismiss()
                            }
                        }
                    }
                    if ("__sunset__" !in currentDeviceIds) {
                        item {
                            SpecialTileRow("Sunset", TileType.HUB_VARIABLE) {
                                onAdd("__sunset__", "Sunset", TileType.HUB_VARIABLE)
                                onDismiss()
                            }
                        }
                    }
                    if ("__civildusk__" !in currentDeviceIds) {
                        item {
                            SpecialTileRow("Civil Dusk", TileType.HUB_VARIABLE) {
                                onAdd("__civildusk__", "Civil Dusk", TileType.HUB_VARIABLE)
                                onDismiss()
                            }
                        }
                    }
                    if ("__astronomicaldusk__" !in currentDeviceIds) {
                        item {
                            SpecialTileRow("Full Dark", TileType.HUB_VARIABLE) {
                                onAdd("__astronomicaldusk__", "Full Dark", TileType.HUB_VARIABLE)
                                onDismiss()
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    item {
                        Text(
                            "Devices",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                items(filteredDevices) { device ->
                    DeviceRow(device = device) {
                        onAdd(device.id, device.label, autoTileType(device))
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialTileRow(label: String, tileType: TileType, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        TileTypeBadge(tileType)
    }
}

@Composable
private fun DeviceRow(device: DeviceState, onClick: () -> Unit) {
    val inferredType = autoTileType(device)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(device.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        TileTypeBadge(inferredType)
    }
}

@Composable
private fun TileTypeBadge(tileType: TileType) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = tileType.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}


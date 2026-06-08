package com.tim.hubitatdash.ui.edit

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.data.model.GroupConfig

private val iconNames = listOf(
    "home", "air", "security", "bedtime", "directions_walk", "fence",
    "emergency", "videocam", "sensor_door", "schedule", "bolt",
    "settings_applications", "star", "doorbell", "lightbulb", "lock",
    "water_drop", "electric_bolt", "thermostat", "people", "badge",
    "category", "garage", "yard"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupSheet(
    existingGroups: List<GroupConfig>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconName: String, parentId: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("home") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val parentOptions = existingGroups

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Create Group", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Text("Icon", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(iconNames) { iconName ->
                    val isSelected = iconName == selectedIcon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.shapes.small
                                ) else Modifier
                            )
                            .clickable { selectedIcon = iconName },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForName(iconName),
                            contentDescription = iconName,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Text("Parent Group (optional)", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = parentOptions.find { it.id == selectedParentId }?.displayName ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Parent") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = { selectedParentId = null; dropdownExpanded = false }
                    )
                    parentOptions.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.displayName) },
                            onClick = { selectedParentId = group.id; dropdownExpanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name.trim(), selectedIcon, selectedParentId)
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Create") }
            }
        }
    }
}


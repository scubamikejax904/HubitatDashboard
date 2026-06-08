package com.tim.hubitatdash.ui.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.data.model.HubVariable
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.common.TilePill
import com.tim.hubitatdash.ui.tiles.common.TileShell

private val readOnlySunVars = setOf("Sunrise", "Sunset", "CivilDusk", "AstronomicalDusk")

@Composable
fun HubVariableTile(
    tile: TileConfig,
    hubVariables: List<HubVariable>,
    onSetVariable: (name: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val varName = tile.hubVarName ?: tile.label
    val isReadOnly = varName in readOnlySunVars
    val currentValue = hubVariables.firstOrNull { it.name == varName }?.value ?: "—"
    var showEdit by remember { mutableStateOf(false) }
    var editValue by remember(currentValue) { mutableStateOf(currentValue) }

    TileShell(title = tile.displayTitle, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = currentValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
            )
        }
        if (!isReadOnly) {
            TilePill(
                label = "Edit",
                isOn = true,
                icon = Icons.Filled.Edit,
                onColor = TileTokens.BlueCold,
                onClick = { showEdit = true }
            )
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit ${tile.label}") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text(varName) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetVariable(varName, editValue)
                    showEdit = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("Cancel") }
            }
        )
    }
}


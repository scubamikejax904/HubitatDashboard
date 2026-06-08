package com.tim.hubitatdash.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubitatTopBar(
    title: String,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    isEditMode: Boolean = false,
    showEditToggle: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    parentGroupLabel: String? = null,
    onParentClick: (() -> Unit)? = null
) {
    TopAppBar(
        navigationIcon = {
            if (parentGroupLabel != null && onParentClick != null) {
                IconButton(onClick = onParentClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to $parentGroupLabel")
                }
            }
        },
        title = {
            if (parentGroupLabel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = parentGroupLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onParentClick?.invoke() }
                    )
                    Text("›", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text(title)
            }
        },
        actions = {
            if (showEditToggle) {
                IconButton(onClick = onToggleEditMode) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditMode) "Done editing" else "Edit"
                    )
                }
            }
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}


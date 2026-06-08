package com.tim.hubitatdash.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tim.hubitatdash.ui.edit.CreateGroupSheet
import com.tim.hubitatdash.ui.edit.iconForName
import com.tim.hubitatdash.viewmodel.GroupEditViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class DrawerGroup(val id: String, val label: String, val icon: ImageVector)

val allDrawerGroups = listOf(
    DrawerGroup("environment", "Environment", Icons.Default.Air),
    DrawerGroup("security-alarm", "Security Alarm", Icons.Default.Security),
    DrawerGroup("night-security", "Night Security", Icons.Default.Lock),
    DrawerGroup("lights", "Lights", Icons.Default.WbIncandescent),
    DrawerGroup("doors-windows", "Doors & Windows", Icons.Default.MeetingRoom),
    DrawerGroup("presence-motion", "Presence & Motion", Icons.Default.People),
    DrawerGroup("perimeter", "Perimeter", Icons.Default.DoorFront),
    DrawerGroup("emergency", "Emergency", Icons.Default.Emergency),
    DrawerGroup("cameras", "Cameras", Icons.Default.CameraAlt),
    DrawerGroup("ring-detections", "Ring Detections", Icons.Default.Notifications),
    DrawerGroup("seasonal", "Seasonal", Icons.Default.WbSunny),
    DrawerGroup("hub-mode", "Hub Mode", Icons.Default.Home),
    DrawerGroup("power-monitor", "Power Monitor", Icons.Default.ElectricBolt),
    DrawerGroup("system", "System", Icons.Default.Settings)
)

@Composable
fun GroupDrawer(
    currentGroupId: String,
    drawerState: DrawerState,
    scope: CoroutineScope,
    onGroupSelected: (String) -> Unit,
    groupEditViewModel: GroupEditViewModel,
    isEditMode: Boolean
) {
    val resolvedGroups by groupEditViewModel.resolvedGroups.collectAsState()
    val customGroups by groupEditViewModel.customGroups.collectAsState()
    val defaultGroupId by groupEditViewModel.defaultGroupId.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }

    val customGroupIds = customGroups.map { it.id }.toSet()
    val topLevelGroupIds = resolvedGroups
        .filter { group -> customGroups.none { it.id == group.id && it.parentId != null } }
        .map { it.id }

    // Map childId -> parentId for quick lookup
    val childToParent = customGroups.filter { it.parentId != null }.associate { it.id to it.parentId!! }
    // Ordered sibling list per parent (order comes from resolvedGroups which already applied childGroupOrder)
    val childrenByParent: Map<String, List<String>> = customGroups
        .filter { it.parentId != null }
        .groupBy { it.parentId!! }
        .mapValues { (_, childData) ->
            val childIds = childData.map { it.id }.toSet()
            resolvedGroups.filter { it.id in childIds }.map { it.id }
        }

    if (showCreateSheet) {
        CreateGroupSheet(
            existingGroups = resolvedGroups,
            onDismiss = { showCreateSheet = false },
            onConfirm = { name, iconName, parentId ->
                groupEditViewModel.addCustomGroup(name, iconName, parentId)
            }
        )
    }

    ModalDrawerSheet {
        Text(
            text = "Hubitat Dashboard",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        if (isEditMode) {
            TextButton(
                onClick = { showCreateSheet = true },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("New Group")
            }
        }
        HorizontalDivider()
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            resolvedGroups.forEach { group ->
                val isCustom = group.id in customGroupIds
                val isChild = customGroups.find { it.id == group.id }?.parentId != null
                val indent = if (isChild) 32.dp else 0.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onGroupSelected(group.id)
                            scope.launch { drawerState.close() }
                        }
                        .padding(start = 16.dp + indent, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = iconForName(group.iconName),
                        contentDescription = group.displayName,
                        modifier = Modifier.size(24.dp),
                        tint = if (group.id == currentGroupId)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = group.displayName,
                        color = if (group.id == currentGroupId)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (isEditMode) {
                        IconButton(
                            onClick = { groupEditViewModel.setDefaultGroup(group.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Set as default",
                                modifier = Modifier.size(18.dp),
                                tint = if (group.id == defaultGroupId)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                    if (isEditMode && !isChild) {
                        val idx = topLevelGroupIds.indexOf(group.id)
                        IconButton(
                            onClick = { groupEditViewModel.moveGroupUp(group.id) },
                            enabled = idx > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Move up",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { groupEditViewModel.moveGroupDown(group.id) },
                            enabled = idx < topLevelGroupIds.lastIndex,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Move down",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (isEditMode && isChild) {
                        val parentId = childToParent[group.id]!!
                        val siblings = childrenByParent[parentId] ?: emptyList()
                        val idx = siblings.indexOf(group.id)
                        IconButton(
                            onClick = { groupEditViewModel.moveChildGroupUp(parentId, group.id) },
                            enabled = idx > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Move up",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { groupEditViewModel.moveChildGroupDown(parentId, group.id) },
                            enabled = idx < siblings.lastIndex,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Move down",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (isEditMode && isCustom) {
                        IconButton(
                            onClick = { groupEditViewModel.removeCustomGroup(group.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


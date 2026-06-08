package com.tim.hubitatdash.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tim.hubitatdash.data.model.TileConfig
import com.tim.hubitatdash.data.model.TileType
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.ui.edit.DevicePickerSheet
import com.tim.hubitatdash.ui.edit.TileTypePickerSheet
import com.tim.hubitatdash.ui.edit.availableTileTypes
import com.tim.hubitatdash.ui.edit.iconForName
import com.tim.hubitatdash.ui.theme.TileTokens
import com.tim.hubitatdash.ui.tiles.MultiDeviceTileCard
import com.tim.hubitatdash.viewmodel.DeviceViewModel
import com.tim.hubitatdash.viewmodel.GroupEditViewModel
import kotlinx.coroutines.launch

private fun normalizeLabel(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")

private fun tileKey(tile: TileConfig): String = when {
    tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName != null -> "hub-variable-${tile.hubVarName}"
    tile.deviceId != null -> tile.deviceId
    else -> tile.tileType.name
}

private fun subgroupStatusCounts(
    tiles: List<com.tim.hubitatdash.data.model.TileConfig>,
    devices: Map<String, com.tim.hubitatdash.data.model.DeviceState>
): Pair<Int, Int> {
    var active = 0
    var inactive = 0
    for (tile in tiles) {
        val deviceId = tile.deviceId?.takeIf { it.isNotBlank() } ?: continue
        val attrs = devices[deviceId]?.attributes ?: continue
        when {
            "switch"   in attrs -> if (attrs["switch"]   == "on")       active++ else inactive++
            "contact"  in attrs -> if (attrs["contact"]  == "open")     active++ else inactive++
            "motion"   in attrs -> if (attrs["motion"]   == "active")   active++ else inactive++
            "presence" in attrs -> if (attrs["presence"] == "present")  active++ else inactive++
            "lock"     in attrs -> if (attrs["lock"]     == "unlocked") active++ else inactive++
        }
    }
    return Pair(active, inactive)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    groupId: String,
    viewModel: DeviceViewModel,
    groupEditViewModel: GroupEditViewModel = hiltViewModel(),
    onNavigateToGroup: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val resolvedGroups by groupEditViewModel.resolvedGroups.collectAsState()
    val customGroups by groupEditViewModel.customGroups.collectAsState()
    val isEditMode by groupEditViewModel.isEditMode.collectAsState()
    val group = resolvedGroups.find { it.id == groupId }

    // Find subgroups — derived from resolvedGroups so childGroupOrder is respected
    val childGroupIds = customGroups.filter { it.parentId == groupId }.map { it.id }.toSet()
    val childGroups = resolvedGroups.filter { it.id in childGroupIds }

    if (group == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Group not found: $groupId")
        }
        return
    }

    val devices by viewModel.devices.collectAsState()
    val hubVariables by viewModel.hubVariables.collectAsState()
    val hsmStatus by viewModel.hsmStatus.collectAsState()
    val modes by viewModel.modes.collectAsState()
    val multiTileConfigs by groupEditViewModel.multiTileConfigs.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    var showDevicePicker by remember { mutableStateOf(false) }
    var typePickTarget by remember { mutableStateOf<Pair<String, DeviceState>?>(null) }
    /** Tile being title-edited: Pair(groupId, tile) */
    var titleEditTarget by remember { mutableStateOf<Pair<String, TileConfig>?>(null) }

    // Derive stable key list from group tiles
    val tileKeys by remember(group.tiles) {
        derivedStateOf { group.tiles.map { tileKey(it) } }
    }

    // Drag-and-drop state — recreated when the tile list identity changes
    val dragState = remember(groupId) {
        DragDropState(tileKeys) { orderedKeys ->
            groupEditViewModel.setTileOrder(groupId, orderedKeys)
        }
    }
    // Keep keys in sync when tiles are added/removed
    remember(tileKeys) { dragState.updateKeys(tileKeys) }
    val scope = rememberCoroutineScope()

    if (showDevicePicker) {
        val currentDeviceIds = group.tiles.mapNotNull { tile ->
            when {
                tile.tileType == TileType.HSM  -> "__hsm__"
                tile.tileType == TileType.MODE -> "__mode__"
                tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName == "Sunrise" -> "__sunrise__"
                tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName == "Sunset"  -> "__sunset__"
                tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName == "CivilDusk" -> "__civildusk__"
                tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName == "AstronomicalDusk" -> "__astronomicaldusk__"
                !tile.deviceId.isNullOrBlank() -> tile.deviceId
                else -> null
            }
        }.toSet()
        DevicePickerSheet(
            groupId = groupId,
            currentDeviceIds = currentDeviceIds,
            devices = devices,
            onDismiss = { showDevicePicker = false },
            onAdd = { deviceId, label, tileType ->
                groupEditViewModel.addDeviceToGroup(groupId, deviceId, label, tileType)
            }
        )
    }

    typePickTarget?.let { (deviceId, device) ->
        TileTypePickerSheet(
            device = device,
            currentType = group.tiles.find { it.deviceId == deviceId }?.tileType
                ?: availableTileTypes(device).firstOrNull()
                ?: return@let,
            onDismiss = { typePickTarget = null },
            onSelect = { newType ->
                groupEditViewModel.setTileTypeOverride(groupId, deviceId, newType)
            }
        )
    }

    titleEditTarget?.let { (grpId, tile) ->
        TitleEditDialog(
            currentTitle = tile.title ?: "",
            deviceLabel = tile.label,
            onDismiss = { titleEditTarget = null },
            onConfirm = { newTitle ->
                val tileId = when {
                    !tile.deviceId.isNullOrBlank() -> tile.deviceId
                    tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName != null -> "hub-variable-${tile.hubVarName}"
                    else -> tile.tileType.name
                }
                groupEditViewModel.setTileTitle(grpId, tileId, newTitle)
                titleEditTarget = null
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.refresh()
                isRefreshing = false
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        if (group.tiles.isEmpty() && !isEditMode) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No devices in this group", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    Text(
                        text = group.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    // Use drag-state-ordered keys so reorder is reflected immediately
                    val orderedTiles = remember(dragState.keys, group.tiles) {
                        val tileMap = group.tiles.associateBy { tileKey(it) }
                        dragState.keys.mapNotNull { tileMap[it] }
                    }
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = TileTokens.GridMinTile),
                        contentPadding = PaddingValues(TileTokens.GridGap),
                        horizontalArrangement = Arrangement.spacedBy(TileTokens.GridGap),
                        verticalItemSpacing = TileTokens.GridGap
                    ) {
                        items(
                            items = orderedTiles,
                            key = { tile -> tileKey(tile) },
                            span = { tile ->
                                if (tile.tileType == TileType.MODE || tile.tileType == TileType.HSM ||
                                    tile.tileType == TileType.MULTI_DEVICE) {
                                    StaggeredGridItemSpan.FullLine
                                } else {
                                    StaggeredGridItemSpan.SingleLane
                                }
                            }
                        ) { tile ->
                            val tileIndex = orderedTiles.indexOf(tile)
                            val tileKeyStr = tileKey(tile)
                            val isDragging = isEditMode && dragState.draggingIndex == tileIndex
                            val device = if (!tile.deviceId.isNullOrBlank()) {
                                devices[tile.deviceId]
                            } else {
                                val norm = normalizeLabel(tile.label)
                                devices.values.find { normalizeLabel(it.label) == norm }
                            }
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        if (isEditMode) {
                                            val b = coords.boundsInWindow()
                                            dragState.bounds[tileIndex] = Rect(b.left, b.top, b.right, b.bottom)
                                        }
                                    }
                                    .graphicsLayer {
                                        if (isDragging) {
                                            translationX = dragState.dragOffset.x
                                            translationY = dragState.dragOffset.y
                                            scaleX = 1.06f
                                            scaleY = 1.06f
                                            shadowElevation = 16f
                                            alpha = 0.92f
                                        }
                                    }
                                    .then(
                                        if (isEditMode) Modifier.pointerInput(tileKeyStr) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { dragState.onDragStart(tileIndex) },
                                                onDrag = { _, delta -> dragState.onDrag(delta) },
                                                onDragEnd = { dragState.onDragEnd() },
                                                onDragCancel = { dragState.onDragCancel() }
                                            )
                                        } else Modifier
                                    )
                            ) {
                                if (tile.tileType == TileType.MULTI_DEVICE) {
                                    MultiDeviceTileCard(
                                        tile = tile,
                                        config = tile.deviceId?.let { multiTileConfigs[it] },
                                        devices = devices,
                                        onCommand = { deviceId, command, value ->
                                            viewModel.sendCommand(deviceId, command, value)
                                        }
                                    )
                                } else {
                                    TileCard(
                                        tile = tile,
                                        device = device,
                                        onCommand = { deviceId, command, value ->
                                            viewModel.sendCommand(deviceId, command, value)
                                        },
                                        hubVariables = hubVariables,
                                        hsmStatus = hsmStatus,
                                        modes = modes,
                                        onSetHsmMode = { mode -> viewModel.setHsmMode(mode) },
                                        onSetMode = { modeId -> viewModel.setMode(modeId) },
                                        onSetVariable = { name, value -> viewModel.setHubVariable(name, value) }
                                    )
                                }
                                if (isEditMode) {
                                    val removableId = when {
                                        !tile.deviceId.isNullOrBlank() -> tile.deviceId
                                        tile.tileType == TileType.HSM  -> "__hsm__"
                                        tile.tileType == TileType.MODE -> "__mode__"
                                        tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName == "Sunrise" -> "__sunrise__"
                                        tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName == "Sunset"  -> "__sunset__"
                                        tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName == "CivilDusk" -> "__civildusk__"
                                        tile.tileType == TileType.HUB_VARIABLE && tile.hubVarName == "AstronomicalDusk" -> "__astronomicaldusk__"
                                        else -> null
                                    }
                                    if (removableId != null) {
                                        IconButton(
                                            onClick = {
                                                groupEditViewModel.removeDeviceFromGroup(groupId, removableId)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        // Title edit button — bottom-start
                                        IconButton(
                                            onClick = { titleEditTarget = groupId to tile },
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit title",
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                    // Show type-change button for real devices only
                                    if (device != null && !tile.deviceId.isNullOrBlank() &&
                                        availableTileTypes(device).size > 1
                                    ) {
                                        IconButton(
                                            onClick = { typePickTarget = tile.deviceId!! to device },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Tune,
                                                contentDescription = "Change type",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Subgroup navigation strip at the bottom
                        if (childGroups.isNotEmpty()) {
                            items(count = 1, span = { StaggeredGridItemSpan.FullLine }) {
                                Column {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(
                                        text = "Subgroups",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    childGroups.forEach { child ->
                                        val (activeCount, inactiveCount) = subgroupStatusCounts(child.tiles, devices)
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                                .clickable { onNavigateToGroup(child.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = iconForName(child.iconName),
                                                    contentDescription = child.displayName,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = child.displayName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (activeCount > 0 || inactiveCount > 0) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        if (activeCount > 0) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(6.dp)
                                                                        .background(
                                                                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                                                            shape = CircleShape
                                                                        )
                                                                )
                                                                Text(
                                                                    text = activeCount.toString(),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                                                )
                                                            }
                                                        }
                                                        if (inactiveCount > 0) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(6.dp)
                                                                        .background(
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                                            shape = CircleShape
                                                                        )
                                                                )
                                                                Text(
                                                                    text = inactiveCount.toString(),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else if (child.tiles.none { !it.deviceId.isNullOrBlank() }) {
                                                    Text(
                                                        text = "empty",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (isEditMode) {
                    ExtendedFloatingActionButton(
                        onClick = { showDevicePicker = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                        text = { Text("Add Device") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleEditDialog(
    currentTitle: String,
    deviceLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tile Title") },
        text = {
            Column {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Custom title") },
                    placeholder = { Text(deviceLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Leave blank to use the device name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


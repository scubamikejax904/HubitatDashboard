package com.tim.hubitatdash.viewmodel

import androidx.lifecycle.ViewModel
import com.tim.hubitatdash.data.model.CustomGroupData
import com.tim.hubitatdash.data.model.GroupConfig
import com.tim.hubitatdash.data.model.MultiTileConfig
import com.tim.hubitatdash.data.model.TileType
import com.tim.hubitatdash.data.repository.DeviceRepository
import com.tim.hubitatdash.data.repository.GroupRepository
import com.tim.hubitatdash.data.repository.SettingsRepository
import com.tim.hubitatdash.ui.edit.autoTileType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val deviceRepository: DeviceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _defaultGroupId = MutableStateFlow(settingsRepository.defaultGroupId)
    val defaultGroupId: StateFlow<String> = _defaultGroupId.asStateFlow()

    val resolvedGroups: StateFlow<List<GroupConfig>> = groupRepository.resolvedGroupsFlow
    val customGroups: StateFlow<List<CustomGroupData>> = groupRepository.customGroups
    val multiTileConfigs: StateFlow<Map<String, MultiTileConfig>> = groupRepository.multiTileConfigs

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun addCustomGroup(name: String, iconName: String, parentId: String? = null) {
        val id = UUID.randomUUID().toString()
        groupRepository.addCustomGroup(CustomGroupData(id = id, displayName = name, iconName = iconName, parentId = parentId))
    }

    fun removeCustomGroup(id: String) = groupRepository.removeCustomGroup(id)

    fun addDeviceToGroup(groupId: String, deviceId: String, label: String, tileType: TileType) {
        groupRepository.addDeviceToGroup(groupId, deviceId)
        val device = deviceRepository.devices.value[deviceId]
        if (device != null && tileType != autoTileType(device)) {
            groupRepository.setTileTypeOverride(groupId, deviceId, tileType)
        }
    }

    fun removeDeviceFromGroup(groupId: String, deviceId: String) =
        groupRepository.removeDeviceFromGroup(groupId, deviceId)

    fun moveGroupUp(id: String) = groupRepository.moveGroupUp(id)
    fun moveGroupDown(id: String) = groupRepository.moveGroupDown(id)
    fun moveChildGroupUp(parentId: String, childId: String) = groupRepository.moveChildGroupUp(parentId, childId)
    fun moveChildGroupDown(parentId: String, childId: String) = groupRepository.moveChildGroupDown(parentId, childId)

    fun setDefaultGroup(id: String) {
        settingsRepository.setDefaultGroupId(id)
        _defaultGroupId.value = id
    }

    fun setTileOrder(groupId: String, orderedIds: List<String>) =
        groupRepository.setTileOrder(groupId, orderedIds)

    fun setTileTypeOverride(groupId: String, deviceId: String, tileType: TileType) =
        groupRepository.setTileTypeOverride(groupId, deviceId, tileType)

    fun setTileTitle(groupId: String, deviceId: String, title: String) =
        groupRepository.setTileTitle(groupId, deviceId, title)
}


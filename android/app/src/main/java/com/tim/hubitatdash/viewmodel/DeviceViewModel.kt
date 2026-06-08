package com.tim.hubitatdash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tim.hubitatdash.data.model.ConnectionStatus
import com.tim.hubitatdash.data.model.ConnectionType
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.HsmMode
import com.tim.hubitatdash.data.model.HubMode
import com.tim.hubitatdash.data.model.HubVariable
import com.tim.hubitatdash.data.repository.ConnectionResolver
import com.tim.hubitatdash.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val connectionResolver: ConnectionResolver
) : ViewModel() {

    val devices: StateFlow<Map<String, DeviceState>> = deviceRepository.devices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val hsmStatus: StateFlow<HsmMode> = deviceRepository.hsmStatus
        .stateIn(viewModelScope, SharingStarted.Eagerly, HsmMode.UNKNOWN)

    val modes: StateFlow<List<HubMode>> = deviceRepository.modes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hubVariables: StateFlow<List<HubVariable>> = deviceRepository.hubVariables
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val connectionStatus: StateFlow<ConnectionStatus> = deviceRepository.connectionStatus
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionStatus.RECONNECTING)

    val connectionError: StateFlow<String?> = deviceRepository.lastError
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeConnection: StateFlow<ConnectionType> = connectionResolver.activeConnection
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionType.UNKNOWN)

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun sendCommand(deviceId: String, command: String, value: String? = null) {
        viewModelScope.launch {
            deviceRepository.sendCommand(deviceId, command, value).onFailure {
                _snackbarMessage.tryEmit("Command failed: ${it.message}")
            }
        }
    }

    fun setHsmMode(mode: String) {
        viewModelScope.launch {
            deviceRepository.setHsmMode(mode).onFailure {
                _snackbarMessage.tryEmit("HSM change failed: ${it.message}")
            }
        }
    }

    fun setMode(modeId: String) {
        viewModelScope.launch {
            deviceRepository.setMode(modeId).onFailure {
                _snackbarMessage.tryEmit("Mode change failed: ${it.message}")
            }
        }
    }

    fun setHubVariable(name: String, value: String) {
        viewModelScope.launch {
            deviceRepository.setHubVariable(name, value).onFailure {
                _snackbarMessage.tryEmit("Variable update failed: ${it.message}")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            deviceRepository.refresh()
        }
    }
}


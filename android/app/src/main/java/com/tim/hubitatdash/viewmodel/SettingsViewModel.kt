package com.tim.hubitatdash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tim.hubitatdash.data.export.GroupExportManager
import com.tim.hubitatdash.data.model.ConnectionMode
import com.tim.hubitatdash.data.repository.ConnectionResolver
import com.tim.hubitatdash.data.repository.SettingsRepository
import com.tim.hubitatdash.ui.settings.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val connectionResolver: ConnectionResolver,
    private val groupExportManager: GroupExportManager,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                localHubIp = settingsRepository.localHubIp,
                makerAppId = settingsRepository.makerAppId,
                makerToken = settingsRepository.makerToken,
                cloudHubId = settingsRepository.cloudHubId,
                connectionModeIndex = when (settingsRepository.connectionMode) {
                    ConnectionMode.LOCAL -> 0
                    ConnectionMode.CLOUD -> 1
                    ConnectionMode.AUTO -> 2
                },
                hubUsername = settingsRepository.hubUsername,
                hubPassword = settingsRepository.hubPassword,
            )
        }
    }

    fun onLocalHubIpChange(value: String) = _uiState.update { it.copy(localHubIp = value, localHubIpError = null) }
    fun onMakerAppIdChange(value: String) = _uiState.update { it.copy(makerAppId = value) }
    fun onMakerTokenChange(value: String) = _uiState.update { it.copy(makerToken = value) }
    fun onCloudHubIdChange(value: String) = _uiState.update { it.copy(cloudHubId = value) }
    fun onConnectionModeChange(index: Int) = _uiState.update { it.copy(connectionModeIndex = index) }
    fun onHubUsernameChange(value: String) = _uiState.update { it.copy(hubUsername = value) }
    fun onHubPasswordChange(value: String) = _uiState.update { it.copy(hubPassword = value) }
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun validate(): Boolean {
        val state = _uiState.value
        if (state.localHubIp.isBlank() && state.cloudHubId.isBlank()) {
            _uiState.update { it.copy(localHubIpError = "Enter at least a Local IP or Cloud Hub ID") }
            return false
        }
        return true
    }

    fun save() {
        if (!validate()) return
        val state = _uiState.value
        val mode = when (state.connectionModeIndex) {
            0 -> ConnectionMode.LOCAL
            1 -> ConnectionMode.CLOUD
            else -> ConnectionMode.AUTO
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            settingsRepository.saveAll(
                localHubIp = state.localHubIp,
                makerAppId = state.makerAppId,
                makerToken = state.makerToken,
                cloudHubId = state.cloudHubId,
                connectionMode = mode,
                hubUsername = state.hubUsername,
                hubPassword = state.hubPassword,
            )
            _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
        }
    }

    /** Returns the canonical export JSON string to write to a file. */
    fun exportConfig(): String = groupExportManager.buildExportJson()

    /** Parses and applies a previously exported JSON config. */
    fun importConfig(json: String) {
        groupExportManager.parseImportJson(json)
            .onSuccess { data ->
                groupExportManager.importConfig(data)
                _uiState.update { it.copy(snackbarMessage = "Config imported successfully") }
            }
            .onFailure { e ->
                _uiState.update { it.copy(snackbarMessage = "Import failed: ${e.message}") }
            }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            val mode = when (state.connectionModeIndex) {
                0 -> ConnectionMode.LOCAL
                1 -> ConnectionMode.CLOUD
                else -> ConnectionMode.AUTO
            }
            settingsRepository.saveAll(
                localHubIp = state.localHubIp,
                makerAppId = state.makerAppId,
                makerToken = state.makerToken,
                cloudHubId = state.cloudHubId,
                connectionMode = mode,
                hubUsername = state.hubUsername,
                hubPassword = state.hubPassword,
            )
            val (success, message) = connectionResolver.testConnection()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    snackbarMessage = if (success) "✓ $message" else "✗ $message"
                )
            }
        }
    }

    /** Constructs the hub base URL (http://host) from the stored local hub IP. */
    private fun localHubBaseUrl(): String {
        val raw = settingsRepository.localHubIp.trim().trimEnd('/')
        return if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://$raw"
    }

    fun pushConfigToHub() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val hubBase = localHubBaseUrl()
                val json = groupExportManager.buildExportJson()
                val username = settingsRepository.hubUsername
                val password = settingsRepository.hubPassword

                withContext(Dispatchers.IO) {
                    val cookie = if (username.isNotBlank() && password.isNotBlank()) {
                        val loginBody = FormBody.Builder()
                            .add("username", username)
                            .add("password", password)
                            .add("submit", "Login")
                            .build()
                        val loginRequest = Request.Builder()
                            .url("$hubBase/login?loginRedirect=/")
                            .post(loginBody)
                            .build()
                        val noRedirectClient = okHttpClient.newBuilder().followRedirects(false).build()
                        val loginResponse = noRedirectClient.newCall(loginRequest).execute()
                        loginResponse.use {
                            it.header("Set-Cookie")?.split(";")?.get(0)
                                ?: error("Hub login failed — check Hub Username and Password in Settings")
                        }
                    } else null

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "uploadFile",
                            "hubitat-dashboard-backup.json",
                            json.toRequestBody("application/octet-stream".toMediaType())
                        )
                        .build()
                    val uploadBuilder = Request.Builder()
                        .url("$hubBase/hub/fileManager/upload")
                        .post(requestBody)
                    if (cookie != null) uploadBuilder.header("Cookie", cookie)
                    val uploadResponse = okHttpClient.newCall(uploadBuilder.build()).execute()
                    uploadResponse.use {
                        if (!it.isSuccessful) error("Upload failed: HTTP ${it.code}")
                    }
                }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, snackbarMessage = "Config pushed to hub") }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, snackbarMessage = "Push failed: ${e.message}") }
            }
        }
    }

    fun pullConfigFromHub() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val hubBase = localHubBaseUrl()
                withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$hubBase/local/hubitat-dashboard-backup.json")
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    response.use {
                        if (!it.isSuccessful) error("File not found on hub: HTTP ${it.code}")
                        it.body?.string() ?: error("Empty response from hub")
                    }
                }
            }.mapCatching { json ->
                groupExportManager.parseImportJson(json).getOrThrow()
            }.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, pendingHubImportData = data) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, snackbarMessage = "Pull failed: ${e.message}") }
            }
        }
    }

    fun confirmHubPull() {
        val data = _uiState.value.pendingHubImportData ?: return
        groupExportManager.importConfig(data)
        // saveSuccess = true triggers onSaveSuccess() in SettingsScreen, which navigates back
        // to the groups screen and refreshes devices — the correct behaviour after an import.
        _uiState.update { it.copy(pendingHubImportData = null, saveSuccess = true) }
    }

    fun cancelHubPull() {
        _uiState.update { it.copy(pendingHubImportData = null) }
    }
}


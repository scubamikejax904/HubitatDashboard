package com.timshubet.hubitatdashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timshubet.hubitatdashboard.data.api.HubitatApiService
import com.timshubet.hubitatdashboard.data.export.GroupExportManager
import com.timshubet.hubitatdashboard.data.model.ConnectionMode
import com.timshubet.hubitatdashboard.data.repository.ConnectionResolver
import com.timshubet.hubitatdashboard.data.repository.SettingsRepository
import com.timshubet.hubitatdashboard.ui.settings.SettingsUiState
import android.util.Base64
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val connectionResolver: ConnectionResolver,
    private val groupExportManager: GroupExportManager,
    private val retrofit: Retrofit
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Pre-populate from saved settings
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
                }
            )
        }
    }

    fun onLocalHubIpChange(value: String) = _uiState.update { it.copy(localHubIp = value, localHubIpError = null) }
    fun onMakerAppIdChange(value: String) = _uiState.update { it.copy(makerAppId = value) }
    fun onMakerTokenChange(value: String) = _uiState.update { it.copy(makerToken = value) }
    fun onCloudHubIdChange(value: String) = _uiState.update { it.copy(cloudHubId = value) }
    fun onConnectionModeChange(index: Int) = _uiState.update { it.copy(connectionModeIndex = index) }
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
                connectionMode = mode
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
            // Save current form values so ConnectionResolver reads them during test
            settingsRepository.saveAll(
                localHubIp = state.localHubIp,
                makerAppId = state.makerAppId,
                makerToken = state.makerToken,
                cloudHubId = state.cloudHubId,
                connectionMode = mode
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

    private fun buildDynamicService(baseUrl: String): HubitatApiService =
        retrofit.newBuilder().baseUrl("$baseUrl/").build().create(HubitatApiService::class.java)

    private fun compressToBase64(json: String): String {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun decompressFromBase64(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        return GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).readText()
    }

    fun pushConfigToHub() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val baseUrl = connectionResolver.resolveBaseUrl()
                val service = buildDynamicService(baseUrl)
                val json = groupExportManager.buildExportJson()
                val compressed = compressToBase64(json)
                if (compressed.length > 1024) error("Compressed config is ${compressed.length} chars — exceeds hub variable limit of 1024. Use File Export instead.")
                val encoded = java.net.URLEncoder.encode(compressed, "UTF-8")
                service.setHubVariable(
                    name = "BackupConfig",
                    value = encoded,
                    token = settingsRepository.makerToken
                )
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
                val baseUrl = connectionResolver.resolveBaseUrl()
                val service = buildDynamicService(baseUrl)
                val vars = service.getHubVariables(settingsRepository.makerToken)
                val value = vars.find { it.name == "BackupConfig" }?.value
                    ?: error("BackupConfig variable not found or empty")
                // Try gzip+base64 first; fall back to raw JSON
                val json = try { decompressFromBase64(value) } catch (_: Exception) { value }
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
        _uiState.update { it.copy(pendingHubImportData = null, snackbarMessage = "Config pulled from hub") }
    }

    fun cancelHubPull() {
        _uiState.update { it.copy(pendingHubImportData = null) }
    }
}

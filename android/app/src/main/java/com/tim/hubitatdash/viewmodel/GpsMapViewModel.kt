package com.tim.hubitatdash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tim.hubitatdash.data.model.GpsDataPoint
import com.tim.hubitatdash.data.repository.GpsMapRepository
import com.tim.hubitatdash.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

enum class GpsMapLayer(val label: String) {
    STREET("Street"),
    SATELLITE("Satellite"),
    HYBRID("Hybrid")
}

data class GpsMapUiState(
    val configured: Boolean = true,
    val data: List<GpsDataPoint> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val startDate: String = "",
    val endDate: String = "",
    val mapLayer: GpsMapLayer = GpsMapLayer.HYBRID,
    val refreshIntervalSeconds: Int = 0
)

@HiltViewModel
class GpsMapViewModel @Inject constructor(
    private val gpsMapRepository: GpsMapRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(
        GpsMapUiState(
            configured = settingsRepository.gpsMapCsvUrl.isNotBlank(),
            startDate = LocalDate.now().format(dateFormatter),
            endDate = LocalDate.now().format(dateFormatter)
        )
    )
    val uiState: kotlinx.coroutines.flow.StateFlow<GpsMapUiState> = _uiState

    init {
        fetchData()
    }

    fun setStartDate(value: String) {
        _uiState.value = _uiState.value.copy(startDate = value)
    }

    fun setEndDate(value: String) {
        _uiState.value = _uiState.value.copy(endDate = value)
    }

    fun clearDates() {
        _uiState.value = _uiState.value.copy(startDate = "", endDate = "")
    }

    fun setMapLayer(layer: GpsMapLayer) {
        _uiState.value = _uiState.value.copy(mapLayer = layer)
    }

    fun setRefreshInterval(seconds: Int) {
        _uiState.value = _uiState.value.copy(refreshIntervalSeconds = seconds.coerceAtLeast(0))
    }

    fun fetchData() {
        val state = _uiState.value
        val startDate = parseDate(state.startDate)
        val endDate = parseDate(state.endDate)
        if ((state.startDate.isNotBlank() && startDate == null) || (state.endDate.isNotBlank() && endDate == null)) {
            _uiState.value = state.copy(error = "Invalid date format. Use YYYY-MM-DD.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching {
                gpsMapRepository.fetchPoints(
                    startDate = startDate,
                    endDate = endDate,
                    csvUrlOverride = settingsRepository.gpsMapCsvUrl
                )
            }.onSuccess { points ->
                _uiState.value = _uiState.value.copy(data = points, loading = false, error = null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = err.message ?: "Failed to load GPS data"
                )
            }
        }
    }

    private fun parseDate(value: String): LocalDate? {
        if (value.isBlank()) return null
        return try {
            LocalDate.parse(value, dateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}


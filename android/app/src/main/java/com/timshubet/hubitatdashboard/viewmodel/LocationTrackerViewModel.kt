package com.timshubet.hubitatdashboard.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.timshubet.hubitatdashboard.data.repository.SettingsRepository
import com.timshubet.hubitatdashboard.service.AlarmScheduler
import com.timshubet.hubitatdashboard.service.BootReceiver
import com.timshubet.hubitatdashboard.service.LocationTrackerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackerUiState(
    val enabled: Boolean = false,
    val interval: Int = 15,
    val appsScriptUrl: String = "",
    val deviceName: String = "",
    val hasFineLocation: Boolean = false,
    val hasBackgroundLocation: Boolean = false,
    val isTesting: Boolean = false
)

@HiltViewModel
class LocationTrackerViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    fun refreshPermissions(context: Context) {
        _uiState.value = _uiState.value.copy(
            hasFineLocation = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED,
            hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    fun setEnabled(enabled: Boolean) {
        settingsRepository.setGpsTrackingEnabled(enabled)
        val context = getApplication<Application>()
        val scheduler = AlarmScheduler(context)

        if (enabled) {
            if (settingsRepository.isGpsTrackerConfigured()) {
                scheduler.scheduleNext(settingsRepository.gpsTrackingInterval)
                syncBootPrefs(context, true, settingsRepository.gpsTrackingInterval)
                Log.d(TAG, "GPS tracking enabled — alarm scheduled")
            } else {
                Log.w(TAG, "Tracking enabled but not configured (missing URL or device name)")
            }
        } else {
            scheduler.cancel()
            syncBootPrefs(context, false, settingsRepository.gpsTrackingInterval)
            Log.d(TAG, "GPS tracking disabled — alarm cancelled")
        }

        _uiState.value = _uiState.value.copy(enabled = enabled)
    }

    fun setInterval(minutes: Int) {
        val clamped = minutes.coerceAtLeast(5)
        settingsRepository.setGpsTrackingInterval(clamped)

        // Re-schedule if currently enabled
        if (settingsRepository.gpsTrackingEnabled) {
            val context = getApplication<Application>()
            AlarmScheduler(context).scheduleNext(clamped)
            syncBootPrefs(context, true, clamped)
        }

        _uiState.value = _uiState.value.copy(interval = clamped)
    }

    fun setAppsScriptUrl(url: String) {
        settingsRepository.setGpsAppsScriptUrl(url.trim())
        _uiState.value = _uiState.value.copy(appsScriptUrl = url)
    }

    fun setDeviceName(name: String) {
        settingsRepository.setGpsDeviceName(name)
        _uiState.value = _uiState.value.copy(deviceName = name)
    }

    fun testNow(context: Context) {
        if (!settingsRepository.isGpsTrackerConfigured()) {
            Log.w(TAG, "Cannot test — tracker not configured")
            return
        }
        _uiState.value = _uiState.value.copy(isTesting = true)
        val intent = Intent(context, LocationTrackerService::class.java)
        ContextCompat.startForegroundService(context, intent)
        // Reset testing flag after a delay
        viewModelScope.launch {
            delay(8000)
            _uiState.value = _uiState.value.copy(isTesting = false)
        }
    }

    private fun loadState(): TrackerUiState {
        val context = getApplication<Application>()
        return TrackerUiState(
            enabled = settingsRepository.gpsTrackingEnabled,
            interval = settingsRepository.gpsTrackingInterval,
            appsScriptUrl = settingsRepository.gpsAppsScriptUrl,
            deviceName = settingsRepository.gpsDeviceName.ifBlank { Build.MODEL },
            hasFineLocation = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED,
            hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    private fun syncBootPrefs(context: Context, enabled: Boolean, interval: Int) {
        context.getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(BootReceiver.KEY_ENABLED, enabled)
            .putInt(BootReceiver.KEY_INTERVAL, interval)
            .apply()
    }

    companion object {
        private const val TAG = "TrackerVM"
    }
}

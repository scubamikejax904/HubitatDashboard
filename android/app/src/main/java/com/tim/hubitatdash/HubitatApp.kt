package com.tim.hubitatdash

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.tim.hubitatdash.data.repository.SettingsRepository
import com.tim.hubitatdash.service.LocationTrackerService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HubitatApp : Application() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()

        // Re-assert the GPS foreground service on every app launch so tracking
        // survives app updates/force-stops. The LocationTrackerService only gets
        // started by the periodic alarm, a boot (BootReceiver), the Test button,
        // or toggling it on — none of which fire when the process is simply
        // recreated after an update. Without this, an enabled tracker stays dead
        // until a reboot or a manual toggle, silently missing trips.
        try {
            if (settingsRepository.gpsTrackingEnabled &&
                settingsRepository.isGpsTrackerConfigured()
            ) {
                val intent = Intent(this, LocationTrackerService::class.java)
                ContextCompat.startForegroundService(this, intent)
                Log.d("GPSTracker", "Re-asserting LocationTrackerService on app launch")
            }
        } catch (e: Exception) {
            Log.e("GPSTracker", "Failed to start tracker on launch: ${e.message}", e)
        }
    }
}

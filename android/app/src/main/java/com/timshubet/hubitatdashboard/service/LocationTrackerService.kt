package com.timshubet.hubitatdashboard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.timshubet.hubitatdashboard.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Foreground service that obtains a single GPS fix via the Fused Location Provider
 * and POSTs the coordinates as JSON to a Google Apps Script Web App URL.
 *
 * Lifecycle:
 * 1. AlarmReceiver starts this service
 * 2. Service calls startForeground() with a persistent notification
 * 3. Gets a GPS fix (high accuracy → fallback to last known)
 * 4. POSTs { timestamp, latitude, longitude, device } to Apps Script
 * 5. Updates notification with result
 * 6. Re-schedules the next alarm via [AlarmScheduler]
 * 7. stopSelf() after a brief delay
 */
@AndroidEntryPoint
class LocationTrackerService : Service() {

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Getting GPS fix…"))

        serviceScope.launch {
            try {
                val location = getDeviceLocation()
                if (location != null) {
                    val lat = "%.6f".format(location.latitude)
                    val lng = "%.6f".format(location.longitude)
                    updateNotification("Sending: $lat, $lng")
                    postLocation(location.latitude, location.longitude)
                    updateNotification("✓ Sent at ${formatTime()}")
                    Log.d(TAG, "Location posted: lat=${location.latitude} lng=${location.longitude}")
                } else {
                    updateNotification("✗ No GPS fix available")
                    Log.w(TAG, "No location available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tracker error: ${e.message}", e)
                updateNotification("✗ Error: ${e.message?.take(60)}")
            } finally {
                // Re-schedule next alarm if tracking is still enabled
                if (settingsRepository.gpsTrackingEnabled) {
                    AlarmScheduler(this@LocationTrackerService)
                        .scheduleNext(settingsRepository.gpsTrackingInterval)
                    syncBootPrefs()
                }
                // Give the notification a moment to show the final state, then stop
                delay(3000)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // region — Location

    private suspend fun getDeviceLocation(): android.location.Location? {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)

            // Try current high-accuracy location first
            val cancellationTokenSource = CancellationTokenSource()
            return try {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "getCurrentLocation failed, trying lastLocation: ${e.message}")
                fusedClient.lastLocation.await()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied: ${e.message}")
            return null
        }
    }

    // endregion

    // region — Network

    private suspend fun postLocation(latitude: Double, longitude: Double) {
        val url = settingsRepository.gpsAppsScriptUrl
        val device = settingsRepository.gpsDeviceName.ifBlank { Build.MODEL }

        if (url.isBlank()) {
            Log.w(TAG, "Apps Script URL is blank — skipping POST")
            return
        }

        val json = JSONObject().apply {
            put("timestamp", Instant.now().toString())
            put("latitude", latitude)
            put("longitude", longitude)
            put("device", device)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) {
                Log.w(TAG, "Apps Script returned HTTP ${it.code}: ${it.body?.string()?.take(200)}")
            }
        }
    }

    // endregion

    // region — Notification

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Tracker",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background GPS location tracking"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Hubitat GPS Tracker")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // endregion

    // region — Helpers

    private fun formatTime(): String {
        return DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
    }

    /**
     * Syncs the tracking enabled/interval flags to a non-encrypted prefs file
     * so [BootReceiver] can read them without the Android Keystore.
     */
    private fun syncBootPrefs() {
        val bootPrefs = getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        bootPrefs.edit()
            .putBoolean(BootReceiver.KEY_ENABLED, settingsRepository.gpsTrackingEnabled)
            .putInt(BootReceiver.KEY_INTERVAL, settingsRepository.gpsTrackingInterval)
            .apply()
    }

    // endregion

    companion object {
        private const val TAG = "GPSTracker"
        private const val CHANNEL_ID = "gps_tracker"
        private const val NOTIFICATION_ID = 9002
    }
}

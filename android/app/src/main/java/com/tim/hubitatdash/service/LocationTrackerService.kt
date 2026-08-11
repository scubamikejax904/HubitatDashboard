package com.tim.hubitatdash.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.tim.hubitatdash.data.repository.AllNotificationEvent
import com.tim.hubitatdash.data.repository.AllNotificationsRepository
import com.tim.hubitatdash.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

// New channel ID forces recreation with IMPORTANCE_MIN (existing channels can't be downgraded)
private const val CHANNEL_ID = "gps_tracker_silent"
private const val NOTIF_ID = 5001

@AndroidEntryPoint
class LocationTrackerService : Service() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var allNotificationsRepository: AllNotificationsRepository
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "GPS Tracker", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground() immediately on Android 9+ for location foreground services
        startForeground(
            NOTIF_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Tracker")
                .setContentText("Tracking location in background")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .build()
        )

        val intervalSeconds = settingsRepository.gpsPollingIntervalSeconds.toLong()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            intervalSeconds * 1000
        ).apply {
            setMinUpdateIntervalMillis(intervalSeconds * 500)
        }.build()

        // FLAG_UPDATE_CURRENT ensures re-registering replaces the previous registration.
        // FLAG_MUTABLE is required: FusedLocationProviderClient writes location extras into
        // the Intent before firing it — an immutable PendingIntent blocks this and fails.
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, LocationBroadcastReceiver::class.java),
            piFlags
        )

        Log.d("GPSTracker", "Calling requestLocationUpdates (interval=${intervalSeconds}s)")
        fusedLocationClient.requestLocationUpdates(locationRequest, pendingIntent)
            .addOnSuccessListener {
                Log.d("GPSTracker", "requestLocationUpdates SUCCESS")
                allNotificationsRepository.addEvent(
                    AllNotificationEvent(Instant.now(), PKG, "GPS State", "Registered persistent updates (interval ${intervalSeconds}s)")
                )
            }
            .addOnFailureListener { e ->
                Log.e("GPSTracker", "requestLocationUpdates FAILED: ${e.message}", e)
                allNotificationsRepository.addEvent(
                    AllNotificationEvent(Instant.now(), PKG, "GPS Error", "Registration failed: ${e.message}")
                )
            }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, LocationBroadcastReceiver::class.java),
            piFlags
        )
        fusedLocationClient.removeLocationUpdates(pendingIntent)
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val PKG = "com.tim.hubitatdash.service.LocationTrackerService"
    }
}

@AndroidEntryPoint
class LocationBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var allNotificationsRepository: AllNotificationsRepository
    @Inject lateinit var okHttpClient: OkHttpClient

    override fun onReceive(context: Context, intent: Intent) {
        val result = LocationResult.extractResult(intent) ?: return
        val location = result.lastLocation ?: return

        // goAsync() extends the broadcast deadline so we can do I/O off the main thread
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                processLocation(context, location)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing location", e)
                log("GPS Error", "Processing failed: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun processLocation(context: Context, location: android.location.Location) {
        // --- Time-of-day gate ---
        val hour = LocalTime.now().hour
        val startHour = settingsRepository.gpsStartHour
        val endHour = settingsRepository.gpsEndHour
        if (hour < startHour || hour > endHour) {
            Log.d(TAG, "Outside time window [$startHour–$endHour] (hour=$hour), skipping")
            return
        }

        val prefs = context.getSharedPreferences(GPS_PREFS, Context.MODE_PRIVATE)
        val lastLat = prefs.getFloat(KEY_LAST_LAT, Float.NaN)
        val lastLng = prefs.getFloat(KEY_LAST_LNG, Float.NaN)
        val lastUploadMs = prefs.getLong(KEY_LAST_UPLOAD_TIME, 0L)

        // --- Distance from last posted location ---
        val distanceMiles = if (lastLat.isNaN() || lastLng.isNaN()) {
            Float.MAX_VALUE // first-ever run — always upload
        } else {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                lastLat.toDouble(), lastLng.toDouble(),
                location.latitude, location.longitude,
                results
            )
            results[0] / METERS_PER_MILE
        }

        // --- Time elapsed since last upload ---
        val elapsedMin = (System.currentTimeMillis() - lastUploadMs) / 60_000.0
        val intervalMin = settingsRepository.gpsTrackingInterval.toDouble()
        val minDist = settingsRepository.gpsMinDistanceMiles

        val movedEnough = distanceMiles >= minDist
        val intervalDue = elapsedMin >= intervalMin

        Log.d(TAG, "dist=${distanceMiles}mi (min=$minDist), elapsed=${elapsedMin}min (interval=$intervalMin), moved=$movedEnough, due=$intervalDue")

        if (!movedEnough && !intervalDue) {
            Log.d(TAG, "Thresholds not met — skipping upload")
            return
        }

        // --- Upload to Google Sheets via Apps Script ---
        val url = settingsRepository.gpsAppsScriptUrl
        if (url.isBlank()) {
            Log.w(TAG, "Apps Script URL not configured — skipping upload")
            return
        }

        val json = JSONObject().apply {
            put("timestamp", Instant.now().toString())
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("device", settingsRepository.gpsDeviceName)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    prefs.edit()
                        .putFloat(KEY_LAST_LAT, location.latitude.toFloat())
                        .putFloat(KEY_LAST_LNG, location.longitude.toFloat())
                        .putLong(KEY_LAST_UPLOAD_TIME, System.currentTimeMillis())
                        .apply()
                    val msg = "Uploaded: ${location.latitude}, ${location.longitude}"
                    Log.d(TAG, msg)
                    log("GPS Upload ✓", msg)
                } else {
                    val msg = "HTTP ${response.code}"
                    Log.w(TAG, "Upload failed: $msg")
                    log("GPS Upload ✗", msg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload network error: ${e.message}", e)
            log("GPS Upload Error", e.message ?: "unknown error")
        }
    }

    private fun log(title: String, text: String) {
        allNotificationsRepository.addEvent(
            AllNotificationEvent(
                timestamp = Instant.now(),
                packageName = "com.tim.hubitatdash.service.LocationBroadcastReceiver",
                title = title,
                text = text
            )
        )
    }

    companion object {
        private const val TAG = "GPSTracker"
        private const val GPS_PREFS = "gps_tracker_prefs"
        private const val KEY_LAST_LAT = "last_latitude"
        private const val KEY_LAST_LNG = "last_longitude"
        private const val KEY_LAST_UPLOAD_TIME = "last_upload_time"
        private const val METERS_PER_MILE = 1609.344f
    }
}

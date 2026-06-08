package com.timshubet.hubitatdashboard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Restores the GPS tracking alarm after a device reboot.
 *
 * Reads a lightweight "enabled" flag from a non-encrypted preference file
 * (encrypted prefs require the Android Keystore which may not be available
 * immediately after boot on some OEM ROMs).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val trackerPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = trackerPrefs.getBoolean(KEY_ENABLED, false)
        val interval = trackerPrefs.getInt(KEY_INTERVAL, 15)

        if (enabled) {
            Log.d(TAG, "Boot completed — rescheduling GPS tracker (interval=${interval}min)")
            AlarmScheduler(context).scheduleNext(interval)

            // Also fire the service immediately so we get a ping soon after boot
            val serviceIntent = Intent(context, LocationTrackerService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            Log.d(TAG, "Boot completed — GPS tracking disabled, skipping")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        const val PREFS_NAME = "gps_tracker_boot"
        const val KEY_ENABLED = "tracking_enabled"
        const val KEY_INTERVAL = "tracking_interval"
    }
}

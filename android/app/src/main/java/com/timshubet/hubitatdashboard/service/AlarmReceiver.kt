package com.timshubet.hubitatdashboard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receiver triggered by [AlarmScheduler]. Starts [LocationTrackerService] as a
 * foreground service so the OS allows GPS + network work even during Doze.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Alarm fired — starting LocationTrackerService")
        val serviceIntent = Intent(context, LocationTrackerService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}

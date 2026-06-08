package com.tim.hubitatdash.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

/**
 * Schedules and cancels the recurring alarm that triggers [LocationTrackerService].
 * Uses [AlarmManager.setExactAndAllowWhileIdle] to survive Doze Mode.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules the next alarm at [intervalMinutes] from now.
     * The minimum interval is enforced at 5 minutes to avoid aggressive battery drain
     * and potential OS throttling of exact alarms on Android 12+.
     */
    fun scheduleNext(intervalMinutes: Int) {
        val effectiveInterval = intervalMinutes.coerceAtLeast(5)
        val triggerAtMs = SystemClock.elapsedRealtime() + effectiveInterval * 60_000L

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
            Log.d(TAG, "Alarm scheduled in ${effectiveInterval} min (trigger at $triggerAtMs)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to set exact alarm (permission denied or S_ALL policy): ${e.message}, falling back to setAndAllowWhileIdle")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
                Log.d(TAG, "Fallback alarm scheduled (inexact) in ${effectiveInterval} min")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule alarm even with fallback: ${e2.message}", e2)
            }
        }
    }

    /**
     * Cancels any pending alarm.
     */
    fun cancel() {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Alarm cancelled")
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val REQUEST_CODE = 9001
    }
}


package com.timshubet.hubitatdashboard.service

import android.app.Notification
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.timshubet.hubitatdashboard.data.repository.ConnectionResolver
import com.timshubet.hubitatdashboard.data.repository.RingEvent
import com.timshubet.hubitatdashboard.data.repository.RingListenerRepository
import com.timshubet.hubitatdashboard.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class RingNotificationListener : NotificationListenerService() {

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var connectionResolver: ConnectionResolver
    @Inject lateinit var ringListenerRepository: RingListenerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        ringListenerRepository.setServiceConnected(true)
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        ringListenerRepository.setServiceConnected(false)
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        ringListenerRepository.setServiceConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != RING_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val combined = "$title $text $bigText".trim()

        Log.d(TAG, "Ring notification: \"$combined\"")

        if (combined.contains(PERSON_DETECTED_TRIGGER, ignoreCase = true)) {
            Log.d(TAG, "Person detected — updating hub variable")
            fireHubitatRequest(combined)
        }
    }

    private fun fireHubitatRequest(notificationText: String) {
        val token = settingsRepository.makerToken

        serviceScope.launch {
            val baseUrl = runCatching { connectionResolver.resolveBaseUrl() }.getOrElse { e ->
                Log.e(TAG, "Failed to resolve hub URL: ${e.message}")
                recordEvent(notificationText, "", success = false, httpCode = null, error = "URL resolution failed: ${e.message}")
                return@launch
            }

            val encodedText = Uri.encode(notificationText)
            val url = "$baseUrl/hubvariables/$HUB_VARIABLE_NAME/$encodedText?access_token=$token"

            val request = runCatching {
                Request.Builder().url(url).build()
            }.getOrElse { e ->
                Log.e(TAG, "Invalid URL: ${e.message}")
                recordEvent(notificationText, url, success = false, httpCode = null, error = "Invalid URL: ${e.message}")
                return@launch
            }

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Hubitat request failed: ${e.message}")
                    recordEvent(notificationText, url, success = false, httpCode = null, error = e.message)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val success = it.isSuccessful
                        if (success) {
                            Log.d(TAG, "Hub variable set (HTTP ${it.code})")
                        } else {
                            Log.w(TAG, "Hub variable set returned HTTP ${it.code}")
                        }
                        recordEvent(notificationText, url, success = success, httpCode = it.code, error = if (!success) it.message else null)
                    }
                }
            })
        }
    }

    private fun recordEvent(
        notificationText: String,
        url: String,
        success: Boolean,
        httpCode: Int?,
        error: String?
    ) {
        ringListenerRepository.addEvent(
            RingEvent(
                timestamp = Instant.now(),
                notificationText = notificationText,
                url = url,
                success = success,
                httpCode = httpCode,
                error = error
            )
        )
    }

    companion object {
        private const val TAG = "RingHub"
        private const val RING_PACKAGE = "com.ringapp"
        private const val PERSON_DETECTED_TRIGGER = "person detected"
        private const val HUB_VARIABLE_NAME = "RingPersonDetected"
    }
}

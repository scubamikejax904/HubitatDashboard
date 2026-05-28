package com.timshubet.hubitatdashboard.service

import android.app.Notification
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.timshubet.hubitatdashboard.data.repository.AllNotificationEvent
import com.timshubet.hubitatdashboard.data.repository.AllNotificationsRepository
import com.timshubet.hubitatdashboard.data.repository.ConnectionResolver
import com.timshubet.hubitatdashboard.data.repository.HubitatEvent
import com.timshubet.hubitatdashboard.data.repository.HubitatNotificationRepository
import com.timshubet.hubitatdashboard.data.repository.RingEvent
import com.timshubet.hubitatdashboard.data.repository.RingListenerRepository
import com.timshubet.hubitatdashboard.data.repository.SettingsRepository
import com.timshubet.hubitatdashboard.data.upload.LogMirrorUploader
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
    @Inject lateinit var hubitatNotificationRepository: HubitatNotificationRepository
    @Inject lateinit var allNotificationsRepository: AllNotificationsRepository
    @Inject lateinit var logMirrorUploader: LogMirrorUploader

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        ringListenerRepository.setServiceConnected(true)
        hubitatNotificationRepository.setServiceConnected(true)
        Log.d(TAG, "Notification listener connected")
        allNotificationsRepository.addEvent(
            AllNotificationEvent(
                timestamp = java.time.Instant.now(),
                packageName = "app.status",
                title = "Listener CONNECTED",
                text = "Service bound — notifications will now be received"
            )
        )

        // Replay any Ring notifications already in the tray that we may have missed
        // while the service was dead (battery optimization kills, etc.)
        try {
            val active = activeNotifications ?: return
            val ringNotifications = active.filter { it.packageName == RING_PACKAGE }
            Log.d(TAG, "Replaying ${ringNotifications.size} active Ring notification(s) on connect")
            if (ringNotifications.isNotEmpty()) {
                allNotificationsRepository.addEvent(
                    AllNotificationEvent(
                        timestamp = java.time.Instant.now(),
                        packageName = "app.status",
                        title = "Replaying ${ringNotifications.size} Ring notification(s) from tray",
                        text = ""
                    )
                )
            }
            ringNotifications.forEach { sbn ->
                try {
                    handleRingNotification(sbn)
                } catch (e: Exception) {
                    Log.e(TAG, "Error replaying Ring notification: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading active notifications on connect: ${e.message}", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        ringListenerRepository.setServiceConnected(false)
        hubitatNotificationRepository.setServiceConnected(false)
        Log.d(TAG, "Notification listener disconnected — requesting rebind")
        allNotificationsRepository.addEvent(
            AllNotificationEvent(
                timestamp = java.time.Instant.now(),
                packageName = "app.status",
                title = "Listener DISCONNECTED",
                text = "Requesting rebind — notifications missed until reconnected"
            )
        )
        requestRebind(android.content.ComponentName(this, RingNotificationListener::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        ringListenerRepository.setServiceConnected(false)
        hubitatNotificationRepository.setServiceConnected(false)
    }

    // Override BOTH signatures — some OEM builds call only the two-arg version
    // and do not delegate to the single-arg version, causing certain notifications
    // (like Ring doorbell) to be silently dropped.
    override fun onNotificationPosted(sbn: StatusBarNotification) = processNotification(sbn)

    override fun onNotificationPosted(
        sbn: StatusBarNotification,
        rankingMap: android.service.notification.NotificationListenerService.RankingMap
    ) = processNotification(sbn)

    private fun processNotification(sbn: StatusBarNotification) {
        val arrivalTime = java.time.Instant.now()

        // Bare entry written BEFORE any extras access, only for Ring.
        // If this appears but the full entry below doesn't, extras extraction threw.
        // If this doesn't appear at all, onNotificationPosted was never called for that notification.
        if (sbn.packageName == RING_PACKAGE) {
            try {
                val notif = sbn.notification
                allNotificationsRepository.addEvent(
                    AllNotificationEvent(
                        timestamp = arrivalTime,
                        packageName = RING_PACKAGE,
                        title = "▶ arrived id=${sbn.id} ch=${notif?.channelId.orEmpty()}",
                        text = "flags=0x${(notif?.flags ?: 0).toString(16)} grp=${notif?.group.orEmpty()} isGroupSummary=${(notif?.flags ?: 0) and Notification.FLAG_GROUP_SUMMARY != 0}"
                    )
                )
            } catch (_: Exception) {}
        }

        try {
            val notification = sbn.notification ?: run {
                allNotificationsRepository.addEvent(
                    AllNotificationEvent(arrivalTime, sbn.packageName, "null notification id=${sbn.id}", "")
                )
                return
            }
            val extras = notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
            val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString().orEmpty()
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
                ?: emptyList()
            val style = extras.getString(Notification.EXTRA_TEMPLATE)?.substringAfterLast('$').orEmpty()

            val rawParts = (listOf(title, text, bigText, subText, summaryText) + textLines)
                .filter { it.isNotBlank() }.distinct()

            // Full extras dump for Ring — reveals any non-standard keys the doorbell uses
            // that we might be missing with our known-key extraction above.
            val extrasDump = if (sbn.packageName == RING_PACKAGE) {
                extras.keySet()
                    .filter { !it.contains("picture", ignoreCase = true) && !it.contains("icon", ignoreCase = true) }
                    .joinToString("; ") { key ->
                        val v = try { extras.get(key)?.toString()?.take(80) } catch (_: Exception) { "?" }
                        "$key=${v.orEmpty()}"
                    }
            } else ""

            val flags = notification.flags
            val groupKey = notification.group?.substringAfterLast('.').orEmpty()
            val diagSuffix = buildString {
                append(" [id=${sbn.id}")
                if (groupKey.isNotBlank()) append(" grp=$groupKey")
                if (flags != 0) append(" flags=0x${flags.toString(16)}")
                if (textLines.isNotEmpty()) append(" lines=${textLines.size}")
                append("]")
            }
            val rawText = buildString {
                if (rawParts.isNotEmpty()) append(rawParts.joinToString(" | "))
                if (extrasDump.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("[extras: $extrasDump]")
                }
            }
            allNotificationsRepository.addEvent(
                AllNotificationEvent(
                    timestamp = arrivalTime,
                    packageName = sbn.packageName,
                    title = (if (style.isNotBlank()) "$title [$style]" else title) + diagSuffix,
                    text = rawText
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing raw notification: ${e.message}", e)
            allNotificationsRepository.addEvent(
                AllNotificationEvent(
                    timestamp = arrivalTime,
                    packageName = "app.error",
                    title = "Raw capture error [id=${sbn.id}]",
                    text = "${e.javaClass.simpleName}: ${e.message}"
                )
            )
        }

        // Ring/Hubitat handling is outside the try-catch so it always runs
        try {
            when (sbn.packageName) {
                RING_PACKAGE -> handleRingNotification(sbn)
                HUBITAT_PACKAGE -> handleHubitatNotification(sbn)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification from ${sbn.packageName}: ${e.message}", e)
            allNotificationsRepository.addEvent(
                AllNotificationEvent(
                    timestamp = java.time.Instant.now(),
                    packageName = "app.error",
                    title = "Handler error [${sbn.packageName}]",
                    text = "${e.javaClass.simpleName}: ${e.message}"
                )
            )
        }
    }

    private fun handleRingNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString().orEmpty()
        // InboxStyle group summaries (Ring combines multiple camera alerts here) store each
        // alert as a separate line in EXTRA_TEXT_LINES.  This is where doorbell text lives
        // when Ring bundles doorbell + stick-up-cam alerts into one group summary.
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
            ?: emptyList()

        // Individual scalar fields — evaluated separately so a camera-name field like "Back Door"
        // never gets concatenated with the person-detection sentence before the trigger check.
        val singleFields = listOf(title, text, bigText, subText, summaryText)
            .filter { it.isNotBlank() }
            .distinct()

        val combined = (singleFields + textLines)
            .distinct()
            .joinToString(" | ")

        Log.d(TAG, "Ring notification: id=${sbn.id} lines=${textLines.size} combined=\"$combined\"")

        if (combined.isBlank()) {
            // Skeleton notification — Ring posts these before populating content.
            // Log it so the Ring screen shows the full lifecycle.
            ringListenerRepository.addEvent(
                RingEvent(
                    timestamp = Instant.now(),
                    notificationText = "(skeleton — no text yet)",
                    url = "",
                    success = false,
                    httpCode = null,
                    error = "blank"
                )
            )
            logMirrorUploader.mirrorRingLog(ringListenerRepository.events.value)
            return
        }

        // If this is an InboxStyle group summary with individual lines, process each line
        // independently so every camera alert gets its own forward decision.
        // Otherwise, check each scalar field separately — this prevents a camera-name field
        // ("Back Door") from being concatenated with the trigger text and forwarded as a
        // combined string like "There is a Person at your Back Door | Back Door".
        val candidates = if (textLines.isNotEmpty()) textLines else singleFields

        var anyForwarded = false
        for (candidate in candidates) {
            val matchedTrigger = FORWARD_TRIGGERS.firstOrNull { candidate.contains(it, ignoreCase = true) }
            if (matchedTrigger != null) {
                Log.d(TAG, "Trigger \"$matchedTrigger\" matched in \"$candidate\" — forwarding")
                fireHubitatRequest(candidate)
                anyForwarded = true
            }
        }

        if (!anyForwarded) {
            // Still log it so the Ring Listener screen shows what Ring is actually sending
            ringListenerRepository.addEvent(
                RingEvent(
                    timestamp = java.time.Instant.now(),
                    notificationText = combined,
                    url = "",
                    success = false,
                    httpCode = null,
                    error = "Not forwarded (no person keyword)"
                )
            )
            logMirrorUploader.mirrorRingLog(ringListenerRepository.events.value)
        }
    }

    private fun handleHubitatNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val body = listOf(text, bigText).filter { it.isNotBlank() }.distinct().joinToString(" | ")

        Log.d(TAG, "Hubitat notification from ${sbn.packageName}: title=\"$title\" body=\"$body\"")

        hubitatNotificationRepository.addEvent(
            HubitatEvent(
                timestamp = Instant.now(),
                packageName = sbn.packageName,
                title = title,
                notificationText = body
            )
        )
        logMirrorUploader.mirrorHubitatLog(hubitatNotificationRepository.events.value)
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
        logMirrorUploader.mirrorRingLog(ringListenerRepository.events.value)
    }

    companion object {
        private const val TAG = "RingHub"
        private const val RING_PACKAGE = "com.ringapp"
        private const val HUBITAT_PACKAGE = "com.hubitat.mobile"
        private val FORWARD_TRIGGERS = listOf("person", "someone", "package")
        private const val HUB_VARIABLE_NAME = "RingPersonDetected"
    }
}

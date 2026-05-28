package com.timshubet.hubitatdashboard.data.upload

import android.util.Log
import com.timshubet.hubitatdashboard.data.repository.HubitatEvent
import com.timshubet.hubitatdashboard.data.repository.RingEvent
import com.timshubet.hubitatdashboard.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogMirrorUploader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun mirrorRingLog(events: List<RingEvent>) {
        val content = JSONArray().apply {
            events.take(MAX_ENTRIES).forEach { event ->
                put(
                    JSONObject().apply {
                        put("ts", event.timestamp.toString())
                        put("text", event.notificationText)
                        put("success", event.success)
                        put("code", event.httpCode ?: JSONObject.NULL)
                        put("err", event.error ?: JSONObject.NULL)
                    }
                )
            }
        }.toString()
        upload(RING_LOG_FILENAME, content)
    }

    fun mirrorHubitatLog(events: List<HubitatEvent>) {
        val content = JSONArray().apply {
            events.take(MAX_ENTRIES).forEach { event ->
                put(
                    JSONObject().apply {
                        put("ts", event.timestamp.toString())
                        put("pkg", event.packageName)
                        put("title", event.title)
                        put("text", event.notificationText)
                    }
                )
            }
        }.toString()
        upload(HUBITAT_LOG_FILENAME, content)
    }

    private fun upload(filename: String, content: String) {
        scope.launch {
            val localHubIp = settingsRepository.localHubIp.trim().trimEnd('/')
            if (localHubIp.isBlank()) return@launch

            val hubBase = "http://$localHubIp"

            runCatching {
                val username = settingsRepository.hubUsername
                val password = settingsRepository.hubPassword
                val cookie = if (username.isNotBlank() && password.isNotBlank()) {
                    val loginBody = FormBody.Builder()
                        .add("username", username)
                        .add("password", password)
                        .add("submit", "Login")
                        .build()
                    val loginRequest = Request.Builder()
                        .url("$hubBase/login?loginRedirect=/")
                        .post(loginBody)
                        .build()
                    val noRedirectClient = okHttpClient.newBuilder().followRedirects(false).build()
                    noRedirectClient.newCall(loginRequest).execute().use {
                        it.header("Set-Cookie")?.split(";")?.getOrNull(0)
                    }
                } else {
                    null
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "uploadFile",
                        filename,
                        content.toRequestBody("application/octet-stream".toMediaType())
                    )
                    .build()
                val uploadBuilder = Request.Builder()
                    .url("$hubBase/hub/fileManager/upload")
                    .post(requestBody)
                if (cookie != null) uploadBuilder.header("Cookie", cookie)

                okHttpClient.newCall(uploadBuilder.build()).execute().use {
                    if (!it.isSuccessful) {
                        error("HTTP ${it.code}")
                    }
                }
            }.onSuccess {
                Log.d(TAG, "Uploaded $filename to hub")
            }.onFailure { error ->
                Log.d(TAG, "Failed to upload $filename: ${error.message}")
            }
        }
    }

    private companion object {
        const val TAG = "LogMirrorUploader"
        const val MAX_ENTRIES = 20
        const val RING_LOG_FILENAME = "ring-log.json"
        const val HUBITAT_LOG_FILENAME = "hubitat-log.json"
    }
}

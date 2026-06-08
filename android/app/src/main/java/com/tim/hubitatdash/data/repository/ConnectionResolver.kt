package com.tim.hubitatdash.data.repository

import com.tim.hubitatdash.data.model.ConnectionMode
import com.tim.hubitatdash.data.model.ConnectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionResolver @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient
) {
    private val _activeConnection = MutableStateFlow(ConnectionType.UNKNOWN)
    val activeConnection: StateFlow<ConnectionType> = _activeConnection.asStateFlow()

    private fun localBaseUrl(): String {
        val raw = settingsRepository.localHubIp.trim().trimEnd('/')
        val appId = settingsRepository.makerAppId.trim()
        val base = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://$raw"
        return "$base/apps/api/$appId"
    }

    private fun cloudBaseUrl(): String {
        val hubId = settingsRepository.cloudHubId.trim()
        val appId = settingsRepository.makerAppId.trim()
        return if (hubId.isNotBlank() && appId.isNotBlank()) {
            "https://cloud.hubitat.com/api/$hubId/apps/$appId"
        } else {
            ""
        }
    }

    suspend fun resolveBaseUrl(): String = withContext(Dispatchers.IO) {
        val token = settingsRepository.makerToken
        when (settingsRepository.connectionMode) {
            ConnectionMode.LOCAL -> {
                _activeConnection.value = ConnectionType.LOCAL
                localBaseUrl()
            }
            ConnectionMode.CLOUD -> {
                _activeConnection.value = ConnectionType.CLOUD
                cloudBaseUrl()
            }
            ConnectionMode.AUTO -> {
                val local = localBaseUrl()
                val probeUrl = "$local/devices/all?access_token=$token"
                val reachable = probeLocal(probeUrl)
                if (reachable) {
                    _activeConnection.value = ConnectionType.LOCAL
                    local
                } else {
                    _activeConnection.value = ConnectionType.CLOUD
                    cloudBaseUrl()
                }
            }
        }
    }

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val baseUrl = resolveBaseUrl()
            val token = settingsRepository.makerToken
            val isCloud = baseUrl.contains("cloud.hubitat.com")
            // Cloud uses GET /devices; local uses HEAD /devices/all
            val testUrl = if (isCloud) "$baseUrl/devices?access_token=$token"
                          else "$baseUrl/devices/all?access_token=$token"
            val testClient = okHttpClient.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
            val request = if (isCloud) Request.Builder().url(testUrl).get().build()
                          else Request.Builder().url(testUrl).head().build()
            val response = testClient.newCall(request).execute()
            response.close()
            if (response.isSuccessful || response.code == 401) {
                Pair(true, "Connected via ${_activeConnection.value} (${testUrl.substringBefore("?").substringBefore("/devices")})")
            } else {
                Pair(false, "HTTP ${response.code} — check URL and token")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Connection failed — check IP and WiFi")
        }
    }

    fun buildSseUrl(baseUrl: String): String {
        val token = settingsRepository.makerToken
        return "$baseUrl/sse?access_token=$token"
    }

    private fun probeLocal(url: String): Boolean {
        return try {
            val probeClient = okHttpClient.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(url).head().build()
            probeClient.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 401
            }
        } catch (e: Exception) {
            false
        }
    }
}


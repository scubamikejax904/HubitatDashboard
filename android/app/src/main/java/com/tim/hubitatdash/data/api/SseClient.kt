package com.tim.hubitatdash.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tim.hubitatdash.data.model.SSEEvent
import com.tim.hubitatdash.data.repository.ConnectionResolver
import com.tim.hubitatdash.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseClient @Inject constructor(
    private val connectionResolver: ConnectionResolver,
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient
) {
    private val _events = MutableSharedFlow<SSEEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SSEEvent> = _events.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectJob: Job? = null
    private val gson = Gson()

    @Volatile private var isRunning = false

    fun connect() {
        if (isRunning) return
        isRunning = true
        connectJob = scope.launch {
            var backoffMs = 1_000L
            while (isRunning) {
                try {
                    val baseUrl = connectionResolver.resolveBaseUrl()
                    val sseUrl = connectionResolver.buildSseUrl(baseUrl)
                    val sseClient = okHttpClient.newBuilder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(0, TimeUnit.SECONDS) // no timeout for SSE stream
                        .build()
                    val request = Request.Builder().url(sseUrl).build()
                    sseClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            _connected.value = false
                            delay(backoffMs)
                            backoffMs = minOf(backoffMs * 2, 30_000L)
                            return@use
                        }
                        backoffMs = 1_000L // reset on successful connect
                        _connected.value = true
                        val source = response.body?.source() ?: return@use
                        while (isRunning && !source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.startsWith("data:")) {
                                val data = line.removePrefix("data:").trim()
                                parseEvent(data)?.let { _events.tryEmit(it) }
                            }
                        }
                        _connected.value = false
                    }
                } catch (e: Exception) {
                    _connected.value = false
                    if (!isRunning) break
                    delay(backoffMs)
                    backoffMs = minOf(backoffMs * 2, 30_000L)
                }
            }
        }
    }

    fun disconnect() {
        isRunning = false
        _connected.value = false
        connectJob?.cancel()
        connectJob = null
    }

    private fun parseEvent(data: String): SSEEvent? {
        return try {
            val json = gson.fromJson(data, JsonObject::class.java)
            // Hubitat SSE format: {"deviceId":"123","name":"switch","value":"on","displayName":"..."}
            val deviceId = json.get("deviceId")?.asString ?: return null
            val attribute = json.get("name")?.asString ?: return null
            val value = json.get("value")?.asString
            SSEEvent(deviceId = deviceId, attribute = attribute, value = value)
        } catch (e: Exception) {
            null
        }
    }
}


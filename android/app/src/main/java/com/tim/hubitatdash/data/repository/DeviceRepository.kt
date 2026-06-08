package com.tim.hubitatdash.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tim.hubitatdash.data.api.HubitatApiService
import com.tim.hubitatdash.data.api.SseClient
import com.tim.hubitatdash.data.model.ConnectionStatus
import com.tim.hubitatdash.data.model.ConnectionType
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.HsmMode
import com.tim.hubitatdash.data.model.HubMode
import com.tim.hubitatdash.data.model.HubVariable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val apiService: HubitatApiService,
    private val sseClient: SseClient,
    private val connectionResolver: ConnectionResolver,
    private val settingsRepository: SettingsRepository,
    private val retrofit: Retrofit,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _devices = MutableStateFlow<Map<String, DeviceState>>(emptyMap())
    val devices: StateFlow<Map<String, DeviceState>> = _devices.asStateFlow()

    private val _hsmStatus = MutableStateFlow(HsmMode.UNKNOWN)
    val hsmStatus: StateFlow<HsmMode> = _hsmStatus.asStateFlow()

    private val _modes = MutableStateFlow<List<HubMode>>(emptyList())
    val modes: StateFlow<List<HubMode>> = _modes.asStateFlow()

    private val _hubVariables = MutableStateFlow<List<HubVariable>>(emptyList())
    val hubVariables: StateFlow<List<HubVariable>> = _hubVariables.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.RECONNECTING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    init {
        scope.launch { refreshWithRetry() }
        scope.launch { collectSseEvents() }
        scope.launch { collectSseConnected() }
        scope.launch { pollHubVariables() }
    }

    private suspend fun refreshWithRetry() {
        var backoffMs = 2_000L
        while (true) {
            if (tryRefresh()) return
            kotlinx.coroutines.delay(backoffMs)
            backoffMs = minOf(backoffMs * 2, 30_000L)
        }
    }

    /** Refresh hub variables once a day — push via Maker API webhook is not possible on Android,
     *  so startup fetch (in tryRefresh) is the primary path; this is a once-a-day safety net. */
    private suspend fun pollHubVariables() {
        val pollIntervalMs = 24 * 60 * 60 * 1_000L
        while (true) {
            kotlinx.coroutines.delay(pollIntervalMs)
            if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                try {
                    val (svc, token, _) = resolvedService()
                    _hubVariables.value = svc.getHubVariables(token)
                } catch (_: Exception) {
                    // Network error — skip this poll cycle
                }
            }
        }
    }

    private var lastResolvedUrl: String = ""

    private suspend fun tryRefresh(): Boolean {
        var step = "init"
        return try {
            step = "resolve"
            val (svc, token, baseUrl) = resolvedService()
            lastResolvedUrl = baseUrl
            val isCloud = baseUrl.contains("cloud.hubitat.com")
            step = "getDevices"
            val deviceList = if (isCloud) fetchCloudDevices(baseUrl.trimEnd('/'), token)
                             else svc.getAllDevices(token)
            _devices.value = deviceList.associateBy { it.id }
            if (!isCloud) {
                // Non-critical extras — only attempt on local (cloud may not support them)
                try {
                    step = "getHsmStatus"
                    _hsmStatus.value = HsmMode.fromApiValue(svc.getHsmStatus(token).hsm)
                } catch (_: Exception) {}
                try {
                    step = "getModes"
                    _modes.value = svc.getModes(token)
                } catch (_: Exception) {}
                try {
                    step = "getHubVariables"
                    _hubVariables.value = svc.getHubVariables(token)
                } catch (_: Exception) {}
                sseClient.connect()
            }
            _connectionStatus.value = ConnectionStatus.CONNECTED
            true
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            _lastError.value = "baseUrl=$lastResolvedUrl [$step] ${e.javaClass.simpleName}: ${e.message}"
            false
        }
    }

    suspend fun refresh() {
        tryRefresh()
    }

    /** Cloud /devices returns summary only (no attributes). Fetch each device individually in parallel. */
    private suspend fun fetchCloudDevices(baseUrl: String, token: String): List<DeviceState> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
        // Step 1: get the list of device IDs
        val listUrl = "$baseUrl/devices?access_token=$token"
        val listBody = okHttpClient.newCall(Request.Builder().url(listUrl).get().build())
            .execute().use { r ->
                if (!r.isSuccessful) throw Exception("HTTP ${r.code} fetching device list")
                r.body?.string() ?: ""
            }
        val summaries = com.google.gson.JsonParser.parseString(listBody)
            .asJsonArray
            .mapNotNull { it.asJsonObject.get("id")?.asString }

        // Step 2: fetch full state for each device in parallel (max 8 concurrent)
        val semaphore = Semaphore(8)
        coroutineScope {
            summaries.map { id ->
                async {
                    semaphore.withPermit {
                        val url = "$baseUrl/devices/$id?access_token=$token"
                        okHttpClient.newCall(Request.Builder().url(url).get().build())
                            .execute().use { r ->
                                if (!r.isSuccessful) return@async null
                                val body = r.body?.string() ?: return@async null
                                gson.fromJson(body, DeviceState::class.java)
                            }
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun resolvedService(): Triple<HubitatApiService, String, String> {
        val baseUrl = connectionResolver.resolveBaseUrl().trimEnd('/') + "/"
        val token = settingsRepository.makerToken
        val svc = retrofit.newBuilder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(HubitatApiService::class.java)
        return Triple(svc, token, baseUrl)
    }

    private suspend fun collectSseConnected() {
        sseClient.connected.collect { isConnected ->
            if (isConnected) _connectionStatus.value = ConnectionStatus.CONNECTED
            // Never flip back to RECONNECTING on SSE drop — REST controls that
        }
    }

    private suspend fun collectSseEvents() {
        sseClient.events.collect { event ->
            _connectionStatus.value = ConnectionStatus.CONNECTED
            val current = _devices.value.toMutableMap()
            val device = current[event.deviceId]
            if (device != null) {
                val updatedAttrs = device.attributes.toMutableMap()
                updatedAttrs[event.attribute] = event.value ?: ""
                current[event.deviceId] = device.copy(attributes = updatedAttrs)
                _devices.value = current
            }
        }
    }

    suspend fun sendCommand(deviceId: String, command: String, value: String? = null): Result<Unit> {
        // Optimistic update: flip the attribute immediately so the UI responds instantly.
        val prevDevices = _devices.value
        applyOptimisticUpdate(deviceId, command, value)
        return try {
            val (svc, token, _) = resolvedService()
            val response = if (value != null) {
                svc.sendCommandWithValue(deviceId, command, value, token)
            } else {
                svc.sendCommand(deviceId, command, token)
            }
            if (response.isSuccessful) Result.success(Unit)
            else {
                _devices.value = prevDevices // revert on failure
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            _devices.value = prevDevices // revert on error
            Result.failure(e)
        }
    }

    /** Immediately updates the in-memory device state for known commands so the UI feels instant. */
    private fun applyOptimisticUpdate(deviceId: String, command: String, value: String?) {
        val device = _devices.value[deviceId] ?: return
        val updated = when (command) {
            "on"  -> device.copy(attributes = device.attributes + ("switch" to "on"))
            "off" -> device.copy(attributes = device.attributes + ("switch" to "off"))
            "lock"   -> device.copy(attributes = device.attributes + ("lock" to "locked"))
            "unlock" -> device.copy(attributes = device.attributes + ("lock" to "unlocked"))
            "setLevel" -> {
                val attrs = device.attributes.toMutableMap()
                if (value != null) attrs["level"] = value
                // If level > 0 assume switch is on
                if (value?.toFloatOrNull()?.let { it > 0 } == true) attrs["switch"] = "on"
                else if (value == "0") attrs["switch"] = "off"
                device.copy(attributes = attrs)
            }
            else -> null
        } ?: return
        _devices.value = _devices.value + (deviceId to updated)
    }

    suspend fun setHsmMode(mode: String): Result<Unit> {
        return try {
            val (svc, token, _) = resolvedService()
            val response = svc.setHsmMode(mode, token)
            if (response.isSuccessful) {
                _hsmStatus.value = HsmMode.fromApiValue(mode)
                Result.success(Unit)
            } else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setMode(modeId: String): Result<Unit> {
        return try {
            val (svc, token, _) = resolvedService()
            val response = svc.setMode(modeId, token)
            if (response.isSuccessful) {
                _modes.value = _modes.value.map { it.copy(active = it.id == modeId) }
                Result.success(Unit)
            } else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setHubVariable(name: String, value: String): Result<Unit> {
        return try {
            val (svc, token, _) = resolvedService()
            val encoded = java.net.URLEncoder.encode(value, "UTF-8")
            val response = svc.setHubVariable(name, encoded, token)
            if (response.isSuccessful) {
                _hubVariables.value = _hubVariables.value.map {
                    if (it.name == name) it.copy(value = value) else it
                }
                Result.success(Unit)
            } else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


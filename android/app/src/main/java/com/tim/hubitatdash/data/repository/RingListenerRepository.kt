package com.tim.hubitatdash.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

data class RingEvent(
    val timestamp: Instant,
    val notificationText: String,
    val url: String,
    val success: Boolean,
    val httpCode: Int?,
    val error: String?
)

@Singleton
class RingListenerRepository @Inject constructor(
    @Named("encrypted") private val prefs: SharedPreferences
) {

    private val _events = MutableStateFlow<List<RingEvent>>(emptyList())
    val events: StateFlow<List<RingEvent>> = _events.asStateFlow()

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private val _isMuted = MutableStateFlow(
        prefs.getBoolean(KEY_RING_MUTED, false)
    )
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun addEvent(event: RingEvent) {
        val current = _events.value.toMutableList()
        current.add(0, event)
        if (current.size > MAX_EVENTS) {
            _events.value = current.subList(0, MAX_EVENTS)
        } else {
            _events.value = current
        }
    }

    fun setServiceConnected(connected: Boolean) {
        _serviceConnected.value = connected
    }

    fun clearEvents() {
        _events.value = emptyList()
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        prefs.edit().putBoolean(KEY_RING_MUTED, muted).apply()
    }

    companion object {
        private const val MAX_EVENTS = 200
        private const val KEY_RING_MUTED = "ring_listener_muted"
    }
}


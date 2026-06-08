package com.tim.hubitatdash.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class HubitatEvent(
    val timestamp: Instant,
    val packageName: String,
    val title: String,
    val notificationText: String
)

@Singleton
class HubitatNotificationRepository @Inject constructor() {

    private val _events = MutableStateFlow<List<HubitatEvent>>(emptyList())
    val events: StateFlow<List<HubitatEvent>> = _events.asStateFlow()

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    fun addEvent(event: HubitatEvent) {
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

    companion object {
        private const val MAX_EVENTS = 50
    }
}


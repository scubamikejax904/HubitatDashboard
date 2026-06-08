package com.tim.hubitatdash.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class AllNotificationEvent(
    val timestamp: Instant,
    val packageName: String,
    val title: String,
    val text: String
)

@Singleton
class AllNotificationsRepository @Inject constructor() {

    private val _events = MutableStateFlow<List<AllNotificationEvent>>(emptyList())
    val events: StateFlow<List<AllNotificationEvent>> = _events.asStateFlow()

    fun addEvent(event: AllNotificationEvent) {
        val current = _events.value.toMutableList()
        current.add(0, event)
        if (current.size > MAX_EVENTS) {
            _events.value = current.subList(0, MAX_EVENTS)
        } else {
            _events.value = current
        }
    }

    fun clearEvents() {
        _events.value = emptyList()
    }

    companion object {
        private const val MAX_EVENTS = 200
    }
}


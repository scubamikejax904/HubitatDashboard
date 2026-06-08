package com.tim.hubitatdash.viewmodel

import androidx.lifecycle.ViewModel
import com.tim.hubitatdash.data.repository.AllNotificationEvent
import com.tim.hubitatdash.data.repository.AllNotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AllNotificationsViewModel @Inject constructor(
    private val allNotificationsRepository: AllNotificationsRepository
) : ViewModel() {

    val events: StateFlow<List<AllNotificationEvent>> = allNotificationsRepository.events

    fun clearEvents() {
        allNotificationsRepository.clearEvents()
    }
}


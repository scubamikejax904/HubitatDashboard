package com.tim.hubitatdash.viewmodel

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.tim.hubitatdash.data.repository.HubitatEvent
import com.tim.hubitatdash.data.repository.HubitatNotificationRepository
import com.tim.hubitatdash.service.RingNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HubitatNotificationViewModel @Inject constructor(
    private val hubitatNotificationRepository: HubitatNotificationRepository
) : ViewModel() {

    val events: StateFlow<List<HubitatEvent>> = hubitatNotificationRepository.events
    val serviceConnected: StateFlow<Boolean> = hubitatNotificationRepository.serviceConnected

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    fun refreshPermission(context: Context) {
        _permissionGranted.value = isNotificationListenerEnabled(context)
    }

    fun clearEvents() {
        hubitatNotificationRepository.clearEvents()
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val cn = ComponentName(context, RingNotificationListener::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(cn.flattenToString()) == true
    }
}


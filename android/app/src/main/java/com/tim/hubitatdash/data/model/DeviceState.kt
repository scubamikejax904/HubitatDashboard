package com.tim.hubitatdash.data.model

data class DeviceState(
    val id: String,
    val label: String,
    val type: String,
    val attributes: Map<String, String> = emptyMap(),
    val commands: List<String>? = null
)


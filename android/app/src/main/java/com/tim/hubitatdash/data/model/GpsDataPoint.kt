package com.tim.hubitatdash.data.model

data class GpsDataPoint(
    val timestamp: String,
    val lat: Double,
    val long: Double,
    val device: String?
)


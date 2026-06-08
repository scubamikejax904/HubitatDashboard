package com.tim.hubitatdash.data.model

data class SSEEvent(
    val deviceId: String,
    val attribute: String,
    val value: String?
)


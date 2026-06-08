package com.tim.hubitatdash.data.model

enum class HsmMode(val apiValue: String, val displayName: String) {
    ARMED_AWAY("armAway", "Armed Away"),
    ARMED_HOME("armHome", "Armed Home"),
    ARMED_NIGHT("armNight", "Armed Night"),
    DISARMED("disarm", "Disarmed"),
    ALL_DISARMED("disarmAll", "All Disarmed"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromApiValue(value: String?): HsmMode =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}


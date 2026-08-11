package com.tim.hubitatdash.ui.shell

object NavRoutes {
    const val SETTINGS = "settings"
    const val RING_LISTENER = "ring_listener"
    const val HUBITAT_LISTENER = "hubitat_listener"
    const val ALL_LOGS = "all_logs"
    const val GPS_TRACKER = "gps_tracker"
    const val GPS_MAP = "gps_map"
    fun group(groupId: String) = "group/$groupId"
    const val GROUP_PATTERN = "group/{groupId}"
    const val DEFAULT_GROUP = "group/"
}

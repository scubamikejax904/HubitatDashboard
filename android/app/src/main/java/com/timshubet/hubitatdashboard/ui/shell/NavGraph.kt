package com.timshubet.hubitatdashboard.ui.shell

object NavRoutes {
    const val SETTINGS = "settings"
    const val RING_LISTENER = "ring_listener"
    fun group(groupId: String) = "group/$groupId"
    const val GROUP_PATTERN = "group/{groupId}"
    const val DEFAULT_GROUP = "group/environment"
}

package com.timshubet.hubitatdashboard.data.model

data class TileConfig(
    val deviceId: String? = null,
    val label: String,
    val tileType: TileType,
    val hubVarName: String? = null,
    /** User-editable display title. null or blank = use [label] (device name). */
    val title: String? = null
) {
    /** Returns the user-editable title if set, otherwise the device label. */
    val displayTitle: String get() = if (title.isNullOrBlank()) label else title
}

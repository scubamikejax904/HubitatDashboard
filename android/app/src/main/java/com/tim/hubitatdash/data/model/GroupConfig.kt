package com.tim.hubitatdash.data.model

data class GroupConfig(
    val id: String,
    val displayName: String,
    val iconName: String,
    val tiles: List<TileConfig>
)


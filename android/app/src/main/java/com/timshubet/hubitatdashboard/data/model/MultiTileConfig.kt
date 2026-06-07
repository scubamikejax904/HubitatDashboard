package com.timshubet.hubitatdashboard.data.model

/**
 * Mirrors the web app's MultiTileConfig interface so multi-device tile
 * configurations survive a web → Android → web export/import round-trip.
 * Android does not render multi-device tiles, but it preserves this data
 * so the web app can reconstruct them on re-import.
 */
data class MultiTileConfig(
    val deviceIds: List<String> = emptyList(),
    val cols: Int = 2,
    val label: String? = null,
    /** Per-device display title overrides within the panel, keyed by deviceId. */
    val labels: Map<String, String>? = null
)

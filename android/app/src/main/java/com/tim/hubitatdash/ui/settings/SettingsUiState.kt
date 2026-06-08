package com.tim.hubitatdash.ui.settings

import com.tim.hubitatdash.data.export.GroupExportData

data class SettingsUiState(
    val localHubIp: String = "",
    val makerAppId: String = "",
    val makerToken: String = "",
    val cloudHubId: String = "",
    val connectionModeIndex: Int = 2, // 0=Local, 1=Cloud, 2=Auto
    val localHubIpError: String? = null,
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val snackbarMessage: String? = null,
    val hubUsername: String = "",
    val hubPassword: String = "",
    /** Non-null when a hub pull has been parsed and is awaiting user confirmation. */
    val pendingHubImportData: GroupExportData? = null
)


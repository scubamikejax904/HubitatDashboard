package com.tim.hubitatdash

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import com.tim.hubitatdash.data.repository.SettingsRepository
import com.tim.hubitatdash.ui.shell.MainScreen
import com.tim.hubitatdash.ui.theme.HubitatDashboardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled by system; user can also grant later in Settings */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAppPermissions()
        setContent {
            val darkTheme = when (settingsRepository.getDarkMode()) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            HubitatDashboardTheme(darkTheme = darkTheme) {
                MainScreen(
                    isConfigured = settingsRepository.isConfigured(),
                    isDarkTheme = darkTheme,
                    onThemeToggle = {
                        val next = when (settingsRepository.getDarkMode()) {
                            "system" -> "dark"
                            "dark" -> "light"
                            else -> "system"
                        }
                        settingsRepository.setDarkMode(next)
                        recreate()
                    }
                )
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}


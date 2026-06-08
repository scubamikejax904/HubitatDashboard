package com.tim.hubitatdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.tim.hubitatdash.data.repository.SettingsRepository
import com.tim.hubitatdash.ui.shell.MainScreen
import com.tim.hubitatdash.ui.theme.HubitatDashboardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}


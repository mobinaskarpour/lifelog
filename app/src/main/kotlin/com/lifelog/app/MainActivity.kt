package com.lifelog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.app.navigation.LifeLogNavHost
import com.lifelog.domain.model.AppSettings
import com.lifelog.domain.model.ThemeMode
import com.lifelog.domain.repository.SettingsRepository
import com.lifelog.ui.navigation.LifeLogRoutes
import com.lifelog.ui.theme.LifeLogTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialSettings = runBlocking { settingsRepository.getSettings().first() }

        setContent {
            val settings by settingsRepository.getSettings().collectAsStateWithLifecycle(
                initialValue = initialSettings,
            )
            var onboardingDone by remember { mutableStateOf(initialSettings.onboardingCompleted) }

            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LifeLogTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColors,
            ) {
                val startDest = if (onboardingDone) {
                    LifeLogRoutes.DASHBOARD
                } else {
                    LifeLogRoutes.ONBOARDING
                }

                LifeLogNavHost(
                    startDestination = startDest,
                    onOnboardingComplete = { onboardingDone = true },
                )
            }
        }
    }
}

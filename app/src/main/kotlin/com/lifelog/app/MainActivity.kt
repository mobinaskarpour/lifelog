package com.lifelog.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.app.navigation.LifeLogNavHost
import com.lifelog.domain.model.ThemeMode
import com.lifelog.domain.repository.SettingsRepository
import com.lifelog.service.TrackingActions
import com.lifelog.ui.navigation.LifeLogRoutes
import com.lifelog.ui.theme.LifeLogTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    private val pendingRouteFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialSettings = runBlocking { settingsRepository.getSettings().first() }
        pendingRouteFlow.value = resolveLaunchRoute(intent, initialSettings.lastOpenedRoute)

        setContent {
            val settings by settingsRepository.getSettings().collectAsStateWithLifecycle(
                initialValue = initialSettings,
            )
            var onboardingDone by remember { mutableStateOf(initialSettings.onboardingCompleted) }
            val pendingRoute by pendingRouteFlow.asStateFlow().collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            val darkTheme =
                when (settings.themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }

            LifeLogTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColors,
            ) {
                val defaultStart =
                    if (onboardingDone) {
                        LifeLogRoutes.DASHBOARD
                    } else {
                        LifeLogRoutes.ONBOARDING
                    }

                LifeLogNavHost(
                    startDestination = defaultStart,
                    pendingRoute = if (onboardingDone) pendingRoute else null,
                    onRouteHandled = { pendingRouteFlow.value = null },
                    onSaveRoute = { route ->
                        scope.launch { settingsRepository.setLastOpenedRoute(route) }
                    },
                    onOnboardingComplete = { onboardingDone = true },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val lastRoute = runBlocking { settingsRepository.getSettings().first().lastOpenedRoute }
        pendingRouteFlow.value = resolveLaunchRoute(intent, lastRoute)
    }

    private fun resolveLaunchRoute(
        intent: Intent,
        fallbackRoute: String,
    ): String? {
        val route = intent.getStringExtra(TrackingActions.EXTRA_ROUTE) ?: fallbackRoute
        return route.takeIf { isValidRoute(it) }
    }

    private fun isValidRoute(route: String): Boolean =
        route in
            setOf(
                LifeLogRoutes.DASHBOARD,
                LifeLogRoutes.TIMELINE,
                LifeLogRoutes.APPS,
                LifeLogRoutes.NOTIFICATIONS,
                LifeLogRoutes.SETTINGS,
                LifeLogRoutes.CALLS,
                LifeLogRoutes.SMS,
                LifeLogRoutes.LOCATION,
                LifeLogRoutes.STATISTICS,
                LifeLogRoutes.SEARCH,
                LifeLogRoutes.ABOUT,
            )
}

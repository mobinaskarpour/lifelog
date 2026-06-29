package com.lifelog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifelog.domain.model.AppSettings
import com.lifelog.domain.model.Language
import com.lifelog.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifelog_settings")

@Singleton
class SettingsDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val THEME = stringPreferencesKey("theme_mode")
            val LANGUAGE = stringPreferencesKey("language")
            val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
            val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
            val LOCATION_TRACKING = booleanPreferencesKey("location_tracking")
            val LOCATION_INTERVAL = intPreferencesKey("location_interval")
            val NOTIFICATION_TRACKING = booleanPreferencesKey("notification_tracking")
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
            val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
            val MONITORING_STARTED_AT = longPreferencesKey("monitoring_started_at")
            val LAST_OPENED_ROUTE = stringPreferencesKey("last_opened_route")
        }

        val settings: Flow<AppSettings> =
            context.dataStore.data.map { prefs ->
                AppSettings(
                    themeMode = prefs[Keys.THEME]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
                    language = prefs[Keys.LANGUAGE]?.let { Language.valueOf(it) } ?: Language.ENGLISH,
                    dynamicColors = prefs[Keys.DYNAMIC_COLORS] ?: true,
                    autoDeleteDays = prefs[Keys.AUTO_DELETE_DAYS] ?: 0,
                    locationTrackingEnabled = prefs[Keys.LOCATION_TRACKING] ?: false,
                    locationIntervalMinutes = prefs[Keys.LOCATION_INTERVAL] ?: 15,
                    notificationTrackingEnabled = prefs[Keys.NOTIFICATION_TRACKING] ?: true,
                    onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                    monitoringEnabled = prefs[Keys.MONITORING_ENABLED] ?: true,
                    monitoringStartedAt = prefs[Keys.MONITORING_STARTED_AT] ?: 0L,
                    lastOpenedRoute = prefs[Keys.LAST_OPENED_ROUTE] ?: "dashboard",
                )
            }

        suspend fun updateSettings(settings: AppSettings) {
            context.dataStore.edit { prefs ->
                prefs[Keys.THEME] = settings.themeMode.name
                prefs[Keys.LANGUAGE] = settings.language.name
                prefs[Keys.DYNAMIC_COLORS] = settings.dynamicColors
                prefs[Keys.AUTO_DELETE_DAYS] = settings.autoDeleteDays
                prefs[Keys.LOCATION_TRACKING] = settings.locationTrackingEnabled
                prefs[Keys.LOCATION_INTERVAL] = settings.locationIntervalMinutes
                prefs[Keys.NOTIFICATION_TRACKING] = settings.notificationTrackingEnabled
                prefs[Keys.ONBOARDING_COMPLETED] = settings.onboardingCompleted
                prefs[Keys.MONITORING_ENABLED] = settings.monitoringEnabled
                prefs[Keys.MONITORING_STARTED_AT] = settings.monitoringStartedAt
                prefs[Keys.LAST_OPENED_ROUTE] = settings.lastOpenedRoute
            }
        }

        suspend fun setOnboardingCompleted(completed: Boolean) {
            context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
        }

        suspend fun setMonitoringEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
        }

        suspend fun setMonitoringStartedAt(timestamp: Long) {
            context.dataStore.edit { it[Keys.MONITORING_STARTED_AT] = timestamp }
        }

        suspend fun setLastOpenedRoute(route: String) {
            context.dataStore.edit { it[Keys.LAST_OPENED_ROUTE] = route }
        }
    }

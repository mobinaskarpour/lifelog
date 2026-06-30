package com.lifelog.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class Language {
    ENGLISH,
    SPANISH,
    FRENCH,
    GERMAN,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: Language = Language.ENGLISH,
    val dynamicColors: Boolean = true,
    val autoDeleteDays: Int = 0,
    val locationTrackingEnabled: Boolean = false,
    val locationIntervalMinutes: Int = 15,
    val notificationTrackingEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val monitoringEnabled: Boolean = true,
    val monitoringStartedAt: Long = 0L,
    val lastOpenedRoute: String = "dashboard",
    val appMessageCaptureEnabled: Boolean = true,
)

package com.lifelog.domain.repository

import com.lifelog.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun updateTheme(themeMode: com.lifelog.domain.model.ThemeMode)
    suspend fun updateLanguage(language: com.lifelog.domain.model.Language)
    suspend fun setOnboardingCompleted(completed: Boolean)
}

interface DashboardRepository {
    fun getDashboardStats(): Flow<com.lifelog.domain.model.DashboardStats>
}

interface ExportRepository {
    suspend fun exportToCsv(): String
    suspend fun exportToJson(): String
    suspend fun exportToPdf(outputPath: String): Boolean
    suspend fun backupDatabase(outputPath: String): Boolean
    suspend fun restoreDatabase(inputPath: String): Boolean
}

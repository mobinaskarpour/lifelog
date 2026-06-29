package com.lifelog.service.di

import com.lifelog.domain.repository.SettingsRepository
import com.lifelog.service.sms.SmsSyncManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MonitoringEntryPoint {
    fun settingsRepository(): SettingsRepository

    fun smsSyncManager(): SmsSyncManager
}

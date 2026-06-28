package com.lifelog.data.di

import com.lifelog.data.repository.AppUsageRepositoryImpl
import com.lifelog.data.repository.BatteryRepositoryImpl
import com.lifelog.data.repository.CallRepositoryImpl
import com.lifelog.data.repository.DashboardRepositoryImpl
import com.lifelog.data.repository.ExportRepositoryImpl
import com.lifelog.data.repository.LocationRepositoryImpl
import com.lifelog.data.repository.NotificationRepositoryImpl
import com.lifelog.data.repository.ScreenEventRepositoryImpl
import com.lifelog.data.repository.SettingsRepositoryImpl
import com.lifelog.data.repository.TimelineRepositoryImpl
import com.lifelog.domain.repository.AppUsageRepository
import com.lifelog.domain.repository.BatteryRepository
import com.lifelog.domain.repository.CallRepository
import com.lifelog.domain.repository.DashboardRepository
import com.lifelog.domain.repository.ExportRepository
import com.lifelog.domain.repository.LocationRepository
import com.lifelog.domain.repository.NotificationRepository
import com.lifelog.domain.repository.ScreenEventRepository
import com.lifelog.domain.repository.SettingsRepository
import com.lifelog.domain.repository.TimelineRepository
import com.lifelog.domain.usecase.CleanupOldLogsUseCase
import com.lifelog.domain.usecase.GetDashboardStatsUseCase
import com.lifelog.domain.usecase.SearchLogsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindTimelineRepository(impl: TimelineRepositoryImpl): TimelineRepository
    @Binds @Singleton abstract fun bindAppUsageRepository(impl: AppUsageRepositoryImpl): AppUsageRepository
    @Binds @Singleton abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds @Singleton abstract fun bindCallRepository(impl: CallRepositoryImpl): CallRepository
    @Binds @Singleton abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
    @Binds @Singleton abstract fun bindBatteryRepository(impl: BatteryRepositoryImpl): BatteryRepository
    @Binds @Singleton abstract fun bindScreenEventRepository(impl: ScreenEventRepositoryImpl): ScreenEventRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository
    @Binds @Singleton abstract fun bindExportRepository(impl: ExportRepositoryImpl): ExportRepository
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides @Singleton
    fun provideGetDashboardStatsUseCase(
        appUsageRepository: AppUsageRepository,
        notificationRepository: NotificationRepository,
        callRepository: CallRepository,
        screenEventRepository: ScreenEventRepository,
        batteryRepository: BatteryRepository,
    ) = GetDashboardStatsUseCase(
        appUsageRepository, notificationRepository, callRepository,
        screenEventRepository, batteryRepository,
    )

    @Provides @Singleton
    fun provideSearchLogsUseCase(timelineRepository: TimelineRepository) =
        SearchLogsUseCase(timelineRepository)

    @Provides @Singleton
    fun provideCleanupOldLogsUseCase(
        timelineRepository: TimelineRepository,
        appUsageRepository: AppUsageRepository,
        notificationRepository: NotificationRepository,
        callRepository: CallRepository,
        locationRepository: LocationRepository,
        batteryRepository: BatteryRepository,
        screenEventRepository: ScreenEventRepository,
    ) = CleanupOldLogsUseCase(
        timelineRepository, appUsageRepository, notificationRepository,
        callRepository, locationRepository, batteryRepository, screenEventRepository,
    )
}

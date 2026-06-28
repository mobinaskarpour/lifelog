package com.lifelog.data.repository

import com.google.gson.Gson
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import com.lifelog.core.dispatcher.DispatcherProvider
import com.lifelog.data.datasource.toDomain
import com.lifelog.data.datasource.toEntity
import com.lifelog.data.preferences.SettingsDataStore
import com.lifelog.database.LifeLogDatabase
import com.lifelog.database.dao.AppUsageDao
import com.lifelog.database.dao.BatteryLogDao
import com.lifelog.database.dao.CallLogDao
import com.lifelog.database.dao.LocationLogDao
import com.lifelog.database.dao.NotificationLogDao
import com.lifelog.database.dao.ScreenEventDao
import com.lifelog.database.dao.TimelineEventDao
import com.lifelog.domain.model.AppSettings
import com.lifelog.domain.model.AppUsage
import com.lifelog.domain.model.BatteryLog
import com.lifelog.domain.model.CallLog
import com.lifelog.domain.model.DashboardStats
import com.lifelog.domain.model.Language
import com.lifelog.domain.model.LocationLog
import com.lifelog.domain.model.NotificationLog
import com.lifelog.domain.model.ScreenEvent
import com.lifelog.domain.model.ThemeMode
import com.lifelog.domain.model.TimelineEvent
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
import com.lifelog.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineRepositoryImpl
    @Inject
    constructor(
        private val dao: TimelineEventDao,
        private val dispatchers: DispatcherProvider,
    ) : TimelineRepository {
        override fun getAllEvents(): Flow<List<TimelineEvent>> = dao.getAll().map { list -> list.map { it.toDomain() } }

        override fun getEventsForDate(date: Long): Flow<List<TimelineEvent>> {
            val end = DateTimeUtils.endOfDay(date)
            return dao.getBetween(date, end + 1).map { list -> list.map { it.toDomain() } }
        }

        override fun getEventsBetween(
            start: Long,
            end: Long,
        ): Flow<List<TimelineEvent>> = dao.getBetween(start, end).map { list -> list.map { it.toDomain() } }

        override fun searchEvents(keyword: String): Flow<List<TimelineEvent>> =
            dao.search(
                keyword,
            ).map { list -> list.map { it.toDomain() } }

        override suspend fun insertEvent(event: TimelineEvent): Long =
            withContext(dispatchers.io) {
                dao.insert(event.toEntity())
            }

        override suspend fun insertEvents(events: List<TimelineEvent>) =
            withContext(dispatchers.io) {
                dao.insertAll(events.map { it.toEntity() })
            }

        override suspend fun deleteOldEvents(beforeTimestamp: Long) =
            withContext(dispatchers.io) {
                dao.deleteBefore(beforeTimestamp)
            }
    }

@Singleton
class AppUsageRepositoryImpl
    @Inject
    constructor(
        private val dao: AppUsageDao,
        private val dispatchers: DispatcherProvider,
    ) : AppUsageRepository {
        override fun getAllUsage(): Flow<List<AppUsage>> = dao.getAll().map { list -> list.map { it.toDomain() } }

        override fun getUsageForDate(date: String): Flow<List<AppUsage>> = dao.getForDate(date).map { list -> list.map { it.toDomain() } }

        override fun getUsageBetween(
            startDate: String,
            endDate: String,
        ): Flow<List<AppUsage>> = dao.getBetween(startDate, endDate).map { list -> list.map { it.toDomain() } }

        override fun getTopApps(limit: Int): Flow<List<AppUsage>> = dao.getTop(limit).map { list -> list.map { it.toDomain() } }

        override suspend fun insertOrUpdateUsage(usage: AppUsage) =
            withContext(dispatchers.io) {
                dao.insert(usage.toEntity())
            }

        override suspend fun deleteOldUsage(beforeDate: String) =
            withContext(dispatchers.io) {
                dao.deleteBefore(beforeDate)
            }
    }

@Singleton
class NotificationRepositoryImpl
    @Inject
    constructor(
        private val dao: NotificationLogDao,
        private val dispatchers: DispatcherProvider,
    ) : NotificationRepository {
        override fun getAllNotifications(): Flow<List<NotificationLog>> = dao.getAll().map { list -> list.map { it.toDomain() } }

        override fun getNotificationsForDate(date: Long): Flow<List<NotificationLog>> {
            val end = DateTimeUtils.endOfDay(date)
            return dao.getForDate(date, end + 1).map { list -> list.map { it.toDomain() } }
        }

        override fun searchNotifications(keyword: String): Flow<List<NotificationLog>> =
            dao.search(keyword).map { list -> list.map { it.toDomain() } }

        override fun getNotificationCountForDate(date: Long): Flow<Int> {
            val end = DateTimeUtils.endOfDay(date)
            return dao.getCountForDate(date, end + 1)
        }

        override suspend fun insertNotification(notification: NotificationLog): Long =
            withContext(dispatchers.io) { dao.insert(notification.toEntity()) }

        override suspend fun deleteOldNotifications(beforeTimestamp: Long) =
            withContext(
                dispatchers.io,
            ) { dao.deleteBefore(beforeTimestamp) }
    }

@Singleton
class CallRepositoryImpl
    @Inject
    constructor(
        private val dao: CallLogDao,
        private val dispatchers: DispatcherProvider,
    ) : CallRepository {
        override fun getAllCalls(): Flow<List<CallLog>> = dao.getAll().map { list -> list.map { it.toDomain() } }

        override fun getCallsForDate(date: Long): Flow<List<CallLog>> {
            val end = DateTimeUtils.endOfDay(date)
            return dao.getForDate(date, end + 1).map { list -> list.map { it.toDomain() } }
        }

        override fun searchCalls(keyword: String): Flow<List<CallLog>> = dao.search(keyword).map { list -> list.map { it.toDomain() } }

        override fun getCallCountForDate(date: Long): Flow<Int> {
            val end = DateTimeUtils.endOfDay(date)
            return dao.getCountForDate(date, end + 1)
        }

        override suspend fun insertCall(call: CallLog): Long = withContext(dispatchers.io) { dao.insert(call.toEntity()) }

        override suspend fun deleteOldCalls(beforeTimestamp: Long) = withContext(dispatchers.io) { dao.deleteBefore(beforeTimestamp) }
    }

@Singleton
class LocationRepositoryImpl
    @Inject
    constructor(
        private val dao: LocationLogDao,
        private val dispatchers: DispatcherProvider,
    ) : LocationRepository {
        override fun getAllLocations(): Flow<List<LocationLog>> = dao.getAll().map { list -> list.map { it.toDomain() } }

        override fun getLocationsBetween(
            start: Long,
            end: Long,
        ): Flow<List<LocationLog>> = dao.getBetween(start, end).map { list -> list.map { it.toDomain() } }

        override suspend fun insertLocation(location: LocationLog): Long = withContext(dispatchers.io) { dao.insert(location.toEntity()) }

        override suspend fun deleteOldLocations(beforeTimestamp: Long) = withContext(dispatchers.io) { dao.deleteBefore(beforeTimestamp) }
    }

@Singleton
class BatteryRepositoryImpl
    @Inject
    constructor(
        private val dao: BatteryLogDao,
        private val dispatchers: DispatcherProvider,
    ) : BatteryRepository {
        override fun getAllBatteryLogs(): Flow<List<BatteryLog>> = dao.getAll().map { list -> list.map { it.toDomain() } }

        override fun getLatestBatteryLog(): Flow<BatteryLog?> = dao.getLatest().map { it?.toDomain() }

        override suspend fun insertBatteryLog(log: BatteryLog): Long = withContext(dispatchers.io) { dao.insert(log.toEntity()) }

        override suspend fun deleteOldBatteryLogs(beforeTimestamp: Long) = withContext(dispatchers.io) { dao.deleteBefore(beforeTimestamp) }
    }

@Singleton
class ScreenEventRepositoryImpl
    @Inject
    constructor(
        private val dao: ScreenEventDao,
        private val dispatchers: DispatcherProvider,
    ) : ScreenEventRepository {
        override fun getAllScreenEvents(): Flow<List<ScreenEvent>> = dao.getAll().map { list -> list.map { it.toDomain() } }

        override fun getUnlockCountForDate(date: Long): Flow<Int> {
            val end = DateTimeUtils.endOfDay(date)
            return dao.getUnlockCountForDate(date, end + 1)
        }

        override suspend fun insertScreenEvent(event: ScreenEvent): Long = withContext(dispatchers.io) { dao.insert(event.toEntity()) }

        override suspend fun deleteOldScreenEvents(beforeTimestamp: Long) =
            withContext(
                dispatchers.io,
            ) { dao.deleteBefore(beforeTimestamp) }
    }

@Singleton
class SettingsRepositoryImpl
    @Inject
    constructor(
        private val dataStore: SettingsDataStore,
        private val dispatchers: DispatcherProvider,
    ) : SettingsRepository {
        override fun getSettings(): Flow<AppSettings> = dataStore.settings

        override suspend fun updateSettings(settings: AppSettings) =
            withContext(dispatchers.io) {
                dataStore.updateSettings(settings)
            }

        override suspend fun updateTheme(themeMode: ThemeMode) =
            withContext(dispatchers.io) {
                val current = dataStore.settings.first()
                dataStore.updateSettings(current.copy(themeMode = themeMode))
            }

        override suspend fun updateLanguage(language: Language) =
            withContext(dispatchers.io) {
                val current = dataStore.settings.first()
                dataStore.updateSettings(current.copy(language = language))
            }

        override suspend fun setOnboardingCompleted(completed: Boolean) =
            withContext(dispatchers.io) {
                dataStore.setOnboardingCompleted(completed)
            }
    }

@Singleton
class DashboardRepositoryImpl
    @Inject
    constructor(
        private val appUsageRepository: AppUsageRepository,
        private val notificationRepository: NotificationRepository,
        private val callRepository: CallRepository,
        private val screenEventRepository: ScreenEventRepository,
        private val batteryRepository: BatteryRepository,
    ) : DashboardRepository {
        override fun getDashboardStats(): Flow<DashboardStats> {
            val today = DateTimeUtils.startOfDay()
            val dateStr = DateTimeUtils.formatDate(today)
            return combine(
                appUsageRepository.getUsageForDate(dateStr),
                notificationRepository.getNotificationCountForDate(today),
                callRepository.getCallCountForDate(today),
                screenEventRepository.getUnlockCountForDate(today),
                batteryRepository.getLatestBatteryLog(),
            ) { usage, notifCount, callCount, unlockCount, battery ->
                DashboardStats(
                    screenTimeMs = usage.sumOf { it.totalDuration },
                    appLaunchCount = usage.sumOf { it.launchCount },
                    topApps = usage.sortedByDescending { it.totalDuration }.take(5),
                    notificationCount = notifCount,
                    callCount = callCount,
                    unlockCount = unlockCount,
                    batteryLevel = battery?.level ?: 0,
                    isCharging = battery?.isCharging ?: false,
                    batteryTemperature = battery?.temperature ?: 0f,
                )
            }
        }
    }

@Singleton
class ExportRepositoryImpl
    @Inject
    constructor(
        private val database: LifeLogDatabase,
        private val dispatchers: DispatcherProvider,
    ) : ExportRepository {
        private val gson = Gson()

        override suspend fun exportToCsv(): String =
            withContext(dispatchers.io) {
                val sb = StringBuilder()
                sb.appendLine("Type,Title,Subtitle,Timestamp")
                database.timelineEventDao().getAllSync().forEach {
                    sb.appendLine("Timeline,${it.title},${it.subtitle},${it.timestamp}")
                }
                database.appUsageDao().getAllSync().forEach {
                    sb.appendLine("App,${it.appName},${it.totalDuration},${it.date}")
                }
                database.notificationLogDao().getAllSync().forEach {
                    sb.appendLine("Notification,${it.appName},${it.title},${it.timestamp}")
                }
                database.callLogDao().getAllSync().forEach {
                    sb.appendLine("Call,${it.phoneNumber},${it.type},${it.timestamp}")
                }
                sb.toString()
            }

        override suspend fun exportToJson(): String =
            withContext(dispatchers.io) {
                val data =
                    mapOf(
                        "timeline" to database.timelineEventDao().getAllSync(),
                        "appUsage" to database.appUsageDao().getAllSync(),
                        "notifications" to database.notificationLogDao().getAllSync(),
                        "calls" to database.callLogDao().getAllSync(),
                        "locations" to database.locationLogDao().getAllSync(),
                        "battery" to database.batteryLogDao().getAllSync(),
                        "screenEvents" to database.screenEventDao().getAllSync(),
                    )
                gson.toJson(data)
            }

        override suspend fun exportToPdf(outputPath: String): Boolean =
            withContext(dispatchers.io) {
                try {
                    val file = File(outputPath)
                    file.parentFile?.mkdirs()
                    val document = Document()
                    PdfWriter.getInstance(document, FileOutputStream(file))
                    document.open()
                    document.add(Paragraph("LifeLog Export Report"))
                    document.add(Paragraph("Generated: ${DateTimeUtils.formatDateTime(System.currentTimeMillis())}"))
                    document.add(Paragraph(" "))
                    database.timelineEventDao().getAllSync().take(100).forEach {
                        document.add(Paragraph("${DateTimeUtils.formatTime(it.timestamp)} - ${it.title}: ${it.subtitle}"))
                    }
                    document.close()
                    true
                } catch (_: Exception) {
                    false
                }
            }

        override suspend fun backupDatabase(outputPath: String): Boolean =
            withContext(dispatchers.io) {
                try {
                    val dbFile = File(database.openHelper.writableDatabase.path ?: return@withContext false)
                    if (!dbFile.exists()) return@withContext false
                    val dest = File(outputPath)
                    dest.parentFile?.mkdirs()
                    dbFile.copyTo(dest, overwrite = true)
                    true
                } catch (_: Exception) {
                    false
                }
            }

        override suspend fun restoreDatabase(inputPath: String): Boolean =
            withContext(dispatchers.io) {
                try {
                    val source = File(inputPath)
                    if (!source.exists()) return@withContext false
                    val dbFile = File(database.openHelper.writableDatabase.path ?: return@withContext false)
                    database.close()
                    source.copyTo(dbFile, overwrite = true)
                    true
                } catch (_: Exception) {
                    false
                }
            }
    }

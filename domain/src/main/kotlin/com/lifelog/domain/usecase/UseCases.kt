package com.lifelog.domain.usecase

import com.lifelog.domain.model.DashboardStats
import com.lifelog.domain.model.SearchFilter
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.repository.AppUsageRepository
import com.lifelog.domain.repository.BatteryRepository
import com.lifelog.domain.repository.CallRepository
import com.lifelog.domain.repository.NotificationRepository
import com.lifelog.domain.repository.ScreenEventRepository
import com.lifelog.domain.repository.SmsRepository
import com.lifelog.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

class GetDashboardStatsUseCase(
    private val appUsageRepository: AppUsageRepository,
    private val notificationRepository: NotificationRepository,
    private val callRepository: CallRepository,
    private val screenEventRepository: ScreenEventRepository,
    private val batteryRepository: BatteryRepository,
) {
    operator fun invoke(): Flow<DashboardStats> {
        val today = startOfDay(System.currentTimeMillis())
        val dateStr = formatDate(today)

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

    private fun startOfDay(timestamp: Long): Long {
        val cal =
            Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        return cal.timeInMillis
    }

    private fun formatDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }
}

class SearchLogsUseCase(
    private val timelineRepository: TimelineRepository,
) {
    operator fun invoke(filter: SearchFilter): Flow<List<TimelineEvent>> {
        return if (filter.keyword.isNotBlank()) {
            timelineRepository.searchEvents(filter.keyword)
        } else if (filter.startDate != null && filter.endDate != null) {
            timelineRepository.getEventsBetween(filter.startDate, filter.endDate)
        } else {
            timelineRepository.getAllEvents()
        }
    }
}

class CleanupOldLogsUseCase(
    private val timelineRepository: TimelineRepository,
    private val appUsageRepository: AppUsageRepository,
    private val notificationRepository: NotificationRepository,
    private val callRepository: CallRepository,
    private val smsRepository: SmsRepository,
    private val unifiedMessageRepository: com.lifelog.domain.repository.UnifiedMessageRepository,
    private val locationRepository: com.lifelog.domain.repository.LocationRepository,
    private val batteryRepository: BatteryRepository,
    private val screenEventRepository: ScreenEventRepository,
) {
    suspend operator fun invoke(daysToKeep: Int) {
        if (daysToKeep <= 0) return
        val cutoff = System.currentTimeMillis() - (daysToKeep.toLong() * 24 * 60 * 60 * 1000)
        timelineRepository.deleteOldEvents(cutoff)
        notificationRepository.deleteOldNotifications(cutoff)
        callRepository.deleteOldCalls(cutoff)
        smsRepository.deleteOldMessages(cutoff)
        unifiedMessageRepository.deleteOldMessages(cutoff)
        locationRepository.deleteOldLocations(cutoff)
        batteryRepository.deleteOldBatteryLogs(cutoff)
        screenEventRepository.deleteOldScreenEvents(cutoff)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = cutoff }
        val dateStr = "${cal.get(
            java.util.Calendar.YEAR,
        )}-${cal.get(java.util.Calendar.MONTH) + 1}-${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
        appUsageRepository.deleteOldUsage(dateStr)
    }
}

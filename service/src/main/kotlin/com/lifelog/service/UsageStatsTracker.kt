package com.lifelog.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.lifelog.domain.model.AppUsage
import com.lifelog.domain.model.ScreenEvent
import com.lifelog.domain.model.ScreenEventType
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.repository.AppUsageRepository
import com.lifelog.domain.repository.ScreenEventRepository
import com.lifelog.domain.repository.TimelineRepository
import com.lifelog.utils.AppUtils
import com.lifelog.utils.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timelineRepository: TimelineRepository,
    private val appUsageRepository: AppUsageRepository,
    private val screenEventRepository: ScreenEventRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastForegroundApp: String? = null

    fun trackUsage() {
        scope.launch {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as? UsageStatsManager ?: return@launch

                val endTime = System.currentTimeMillis()
                val startTime = endTime - 60_000
                val events = usageStatsManager.queryEvents(startTime, endTime)
                val event = UsageEvents.Event()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            handleAppOpened(event.packageName, event.timeStamp)
                        }
                        UsageEvents.Event.ACTIVITY_PAUSED -> {
                            handleAppClosed(event.packageName, event.timeStamp)
                        }
                        UsageEvents.Event.SCREEN_INTERACTIVE -> {
                            logScreenEvent(ScreenEventType.SCREEN_ON, event.timeStamp)
                        }
                        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                            logScreenEvent(ScreenEventType.SCREEN_OFF, event.timeStamp)
                        }
                        UsageEvents.Event.KEYGUARD_HIDDEN -> {
                            logScreenEvent(ScreenEventType.DEVICE_UNLOCK, event.timeStamp)
                            logTimeline(
                                TimelineEventType.PHONE_UNLOCKED,
                                "Phone Unlocked",
                                "Device unlocked",
                                event.timeStamp,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error tracking usage stats")
            }
        }
    }

    private suspend fun handleAppOpened(packageName: String, timestamp: Long) {
        if (packageName == context.packageName) return
        lastForegroundApp = packageName
        val appName = AppUtils.getAppName(context, packageName)
        logTimeline(
            TimelineEventType.APP_OPENED,
            "$appName Opened",
            packageName,
            timestamp,
            packageName,
            0xFF2196F3,
        )
        val date = DateTimeUtils.formatDate(timestamp)
        val usage = AppUsage(
            appName = appName,
            packageName = packageName,
            firstOpen = timestamp,
            lastClose = timestamp,
            totalDuration = 0,
            launchCount = 1,
            date = date,
        )
        appUsageRepository.insertOrUpdateUsage(usage)
    }

    private suspend fun handleAppClosed(packageName: String, timestamp: Long) {
        if (packageName == context.packageName) return
        val appName = AppUtils.getAppName(context, packageName)
        logTimeline(
            TimelineEventType.APP_CLOSED,
            "$appName Closed",
            packageName,
            timestamp,
            packageName,
            0xFF607D8B,
        )
    }

    private suspend fun logScreenEvent(type: ScreenEventType, timestamp: Long) {
        screenEventRepository.insertScreenEvent(ScreenEvent(type = type, timestamp = timestamp))
        val (eventType, title) = when (type) {
            ScreenEventType.SCREEN_ON -> TimelineEventType.SCREEN_ON to "Screen On"
            ScreenEventType.SCREEN_OFF -> TimelineEventType.SCREEN_OFF to "Screen Off"
            ScreenEventType.DEVICE_UNLOCK -> return
        }
        logTimeline(eventType, title, "", timestamp)
    }

    private suspend fun logTimeline(
        type: TimelineEventType,
        title: String,
        subtitle: String,
        timestamp: Long,
        packageName: String? = null,
        colorArgb: Long = 0xFF6200EE,
    ) {
        timelineRepository.insertEvent(
            TimelineEvent(
                type = type,
                title = title,
                subtitle = subtitle,
                timestamp = timestamp,
                packageName = packageName,
                colorArgb = colorArgb,
            ),
        )
    }
}

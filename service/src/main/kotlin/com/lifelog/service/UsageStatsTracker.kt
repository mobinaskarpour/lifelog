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
class UsageStatsTracker
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val timelineRepository: TimelineRepository,
        private val appUsageRepository: AppUsageRepository,
        private val screenEventRepository: ScreenEventRepository,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        private val activeSessions = mutableMapOf<String, Long>()
        private var lastUnlockTimestamp = 0L

        fun trackUsage() {
            scope.launch {
                try {
                    val usageStatsManager =
                        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                            ?: return@launch

                    val endTime = System.currentTimeMillis()
                    val lastProcessed = prefs.getLong(KEY_LAST_EVENT_TIME, endTime - LOOKBACK_MS)
                    val startTime = (lastProcessed - OVERLAP_MS).coerceAtLeast(endTime - MAX_LOOKBACK_MS)
                    val events = usageStatsManager.queryEvents(startTime, endTime)
                    val event = UsageEvents.Event()

                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        when (event.eventType) {
                            UsageEvents.Event.MOVE_TO_FOREGROUND,
                            UsageEvents.Event.ACTIVITY_RESUMED,
                            -> handleAppForeground(event.packageName, event.timeStamp)
                            UsageEvents.Event.MOVE_TO_BACKGROUND,
                            UsageEvents.Event.ACTIVITY_PAUSED,
                            -> handleAppBackground(event.packageName, event.timeStamp)
                            UsageEvents.Event.SCREEN_INTERACTIVE -> logScreenEvent(ScreenEventType.SCREEN_ON, event.timeStamp)
                            UsageEvents.Event.SCREEN_NON_INTERACTIVE ->
                                logScreenEvent(ScreenEventType.SCREEN_OFF, event.timeStamp)
                            UsageEvents.Event.KEYGUARD_HIDDEN -> logUnlock(event.timeStamp)
                        }
                    }
                    prefs.edit().putLong(KEY_LAST_EVENT_TIME, endTime).apply()
                } catch (e: Exception) {
                    Timber.e(e, "Error tracking usage stats")
                }
            }
        }

        private suspend fun handleAppForeground(
            packageName: String,
            timestamp: Long,
        ) {
            if (packageName == context.packageName || !AppUtils.shouldTrackPackage(packageName)) return
            if (activeSessions.containsKey(packageName)) return

            activeSessions[packageName] = timestamp
            val appName = AppUtils.getAppName(context, packageName)
            val date = DateTimeUtils.formatDate(timestamp)

            logTimeline(
                TimelineEventType.APP_OPENED,
                appName,
                packageName,
                timestamp,
                packageName,
                0xFF2196F3,
            )

            appUsageRepository.insertOrUpdateUsage(
                AppUsage(
                    appName = appName,
                    packageName = packageName,
                    firstOpen = timestamp,
                    lastOpen = timestamp,
                    lastClose = 0,
                    totalDuration = 0,
                    launchCount = 1,
                    date = date,
                ),
            )
        }

        private suspend fun handleAppBackground(
            packageName: String,
            timestamp: Long,
        ) {
            if (packageName == context.packageName || !AppUtils.shouldTrackPackage(packageName)) return
            val sessionStart = activeSessions.remove(packageName) ?: return
            val duration = (timestamp - sessionStart).coerceAtLeast(0)
            val appName = AppUtils.getAppName(context, packageName)
            val date = DateTimeUtils.formatDate(sessionStart)

            logTimeline(
                TimelineEventType.APP_CLOSED,
                appName,
                packageName,
                timestamp,
                packageName,
                0xFF607D8B,
            )

            appUsageRepository.insertOrUpdateUsage(
                AppUsage(
                    appName = appName,
                    packageName = packageName,
                    firstOpen = sessionStart,
                    lastOpen = sessionStart,
                    lastClose = timestamp,
                    totalDuration = duration,
                    launchCount = 0,
                    date = date,
                ),
            )
        }

        private suspend fun logUnlock(timestamp: Long) {
            if (timestamp - lastUnlockTimestamp < UNLOCK_DEBOUNCE_MS) return
            lastUnlockTimestamp = timestamp
            logScreenEvent(ScreenEventType.DEVICE_UNLOCK, timestamp)
            logTimeline(
                TimelineEventType.PHONE_UNLOCKED,
                "Phone Unlocked",
                "Device unlocked",
                timestamp,
                colorArgb = 0xFF4CAF50,
            )
        }

        private suspend fun logScreenEvent(
            type: ScreenEventType,
            timestamp: Long,
        ) {
            screenEventRepository.insertScreenEvent(ScreenEvent(type = type, timestamp = timestamp))
            val (eventType, title) =
                when (type) {
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
            val displayTitle =
                if (type == TimelineEventType.APP_OPENED) {
                    "$title Opened"
                } else if (type == TimelineEventType.APP_CLOSED) {
                    "$title Closed"
                } else {
                    title
                }
            timelineRepository.insertEvent(
                TimelineEvent(
                    type = type,
                    title = displayTitle,
                    subtitle = subtitle,
                    timestamp = timestamp,
                    packageName = packageName,
                    colorArgb = colorArgb,
                ),
            )
        }

        companion object {
            private const val PREFS_NAME = "usage_tracker"
            private const val KEY_LAST_EVENT_TIME = "last_event_time"
            private const val LOOKBACK_MS = 120_000L
            private const val OVERLAP_MS = 5_000L
            private const val MAX_LOOKBACK_MS = 300_000L
            private const val UNLOCK_DEBOUNCE_MS = 2_000L
        }
    }

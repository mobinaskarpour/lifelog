package com.lifelog.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.repository.TimelineRepository
import com.lifelog.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LifeLogAccessibilityService : AccessibilityService() {
    @Inject lateinit var timelineRepository: TimelineRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentEventKeys = LinkedHashMap<String, Long>()
    private var lastForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("LifeLog accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowChange(event)
            AccessibilityEvent.TYPE_VIEW_CLICKED ->
                logInteraction(event, TimelineEventType.VIEW_CLICKED, "Tap")
            AccessibilityEvent.TYPE_VIEW_SCROLLED ->
                logInteraction(event, TimelineEventType.VIEW_SCROLLED, "Scroll")
        }
    }

    override fun onInterrupt() {
        Timber.d("LifeLog accessibility service interrupted")
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (!AppUtils.shouldTrackPackage(packageName) || packageName == this.packageName) return
        if (packageName == lastForegroundPackage) return

        val previous = lastForegroundPackage
        if (previous != null && AppUtils.shouldTrackPackage(previous)) {
            logAppEvent(previous, TimelineEventType.APP_CLOSED, event.eventTime)
        }

        lastForegroundPackage = packageName
        logAppEvent(packageName, TimelineEventType.APP_OPENED, event.eventTime)
        logWindowChange(packageName, event.eventTime)
    }

    private fun logWindowChange(
        packageName: String,
        timestamp: Long,
    ) {
        val key = "WINDOW:$packageName"
        if (!shouldLog(key, timestamp)) return
        val appName = AppUtils.getAppName(this, packageName)
        logTimeline(
            type = TimelineEventType.WINDOW_CHANGED,
            title = appName,
            subtitle = "Window changed",
            timestamp = timestamp,
            packageName = packageName,
            colorArgb = 0xFF3F51B5,
        )
    }

    private fun logInteraction(
        event: AccessibilityEvent,
        type: TimelineEventType,
        label: String,
    ) {
        val packageName = event.packageName?.toString() ?: return
        if (!AppUtils.shouldTrackPackage(packageName)) return
        val key = "${type.name}:$packageName"
        if (!shouldLog(key, event.eventTime)) return
        val appName = AppUtils.getAppName(this, packageName)
        logTimeline(
            type = type,
            title = "$appName $label",
            subtitle = event.className?.toString().orEmpty(),
            timestamp = event.eventTime,
            packageName = packageName,
            colorArgb = if (type == TimelineEventType.VIEW_CLICKED) 0xFF9C27B0 else 0xFF673AB7,
        )
    }

    private fun logAppEvent(
        packageName: String,
        type: TimelineEventType,
        timestamp: Long,
    ) {
        val key = "${type.name}:$packageName"
        if (!shouldLog(key, timestamp)) return
        val appName = AppUtils.getAppName(this, packageName)
        val title =
            when (type) {
                TimelineEventType.APP_OPENED -> "$appName Opened"
                TimelineEventType.APP_CLOSED -> "$appName Closed"
                else -> appName
            }
        logTimeline(
            type = type,
            title = title,
            subtitle = packageName,
            timestamp = timestamp,
            packageName = packageName,
            colorArgb = if (type == TimelineEventType.APP_OPENED) 0xFF2196F3 else 0xFF607D8B,
        )
    }

    private fun shouldLog(
        key: String,
        timestamp: Long,
    ): Boolean {
        val last = recentEventKeys[key]
        if (last != null && timestamp - last < EVENT_DEBOUNCE_MS) return false
        recentEventKeys[key] = timestamp
        if (recentEventKeys.size > MAX_RECENT_EVENTS) {
            recentEventKeys.entries.firstOrNull()?.let { recentEventKeys.remove(it.key) }
        }
        return true
    }

    private fun logTimeline(
        type: TimelineEventType,
        title: String,
        subtitle: String,
        timestamp: Long,
        packageName: String? = null,
        colorArgb: Long = 0xFF6200EE,
    ) {
        scope.launch {
            try {
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
            } catch (e: Exception) {
                Timber.e(e, "Error logging accessibility event")
            }
        }
    }

    companion object {
        private const val EVENT_DEBOUNCE_MS = 1_500L
        private const val MAX_RECENT_EVENTS = 100
    }
}

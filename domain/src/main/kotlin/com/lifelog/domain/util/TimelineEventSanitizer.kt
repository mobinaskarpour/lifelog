package com.lifelog.domain.util

import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType

object TimelineEventSanitizer {
    fun sanitize(events: List<TimelineEvent>): List<TimelineEvent> = events.mapNotNull { sanitizeOne(it) }

    fun sanitizeOne(event: TimelineEvent): TimelineEvent? {
        if (event.timestamp < 0) return null
        return event.copy(
            title = event.title.ifBlank { defaultTitle(event.type) },
            subtitle = event.subtitle,
            timestamp = event.timestamp,
            packageName = event.packageName?.takeIf { it.isNotBlank() },
            colorArgb = event.colorArgb.coerceIn(0L, 0xFFFFFFFFL),
        )
    }

    fun stableKey(
        event: TimelineEvent,
        index: Int,
    ): String = "${event.id}_${event.timestamp}_${event.type.name}_${event.packageName.orEmpty()}_$index"

    private fun defaultTitle(type: TimelineEventType): String =
        when (type) {
            TimelineEventType.APP_OPENED -> "App session"
            TimelineEventType.APP_CLOSED -> "App closed"
            TimelineEventType.NOTIFICATION_RECEIVED -> "Notification"
            TimelineEventType.PHONE_UNLOCKED -> "Phone unlocked"
            else -> "Activity"
        }
}

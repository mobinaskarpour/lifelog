package com.lifelog.domain.model

enum class TimelineEventType {
    PHONE_UNLOCKED,
    APP_OPENED,
    APP_CLOSED,
    INCOMING_CALL,
    OUTGOING_CALL,
    MISSED_CALL,
    CALL_ENDED,
    NOTIFICATION_RECEIVED,
    SCREEN_ON,
    SCREEN_OFF,
    BATTERY_CHANGED,
    WIFI_CONNECTED,
    WIFI_DISCONNECTED,
    MOBILE_DATA_ON,
    MOBILE_DATA_OFF,
    BLUETOOTH_ON,
    BLUETOOTH_OFF,
    AIRPLANE_MODE_ON,
    AIRPLANE_MODE_OFF,
    LOCATION_UPDATE,
    WINDOW_CHANGED,
    VIEW_CLICKED,
    VIEW_SCROLLED,
    ;

    companion object {
        fun fromString(value: String?): TimelineEventType? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name == value }
        }
    }
}

data class TimelineEvent(
    val id: Long = 0,
    val type: TimelineEventType,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val packageName: String? = null,
    val colorArgb: Long = 0xFF6200EE,
)

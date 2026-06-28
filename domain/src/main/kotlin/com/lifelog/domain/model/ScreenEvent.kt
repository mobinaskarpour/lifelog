package com.lifelog.domain.model

enum class ScreenEventType {
    SCREEN_ON,
    SCREEN_OFF,
    DEVICE_UNLOCK,
}

data class ScreenEvent(
    val id: Long = 0,
    val type: ScreenEventType,
    val timestamp: Long,
)

package com.lifelog.domain.model

data class BatteryLog(
    val id: Long = 0,
    val level: Int,
    val isCharging: Boolean,
    val temperature: Float,
    val isPowerConnected: Boolean,
    val timestamp: Long,
)

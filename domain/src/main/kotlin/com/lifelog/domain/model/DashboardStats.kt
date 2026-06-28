package com.lifelog.domain.model

data class DashboardStats(
    val screenTimeMs: Long = 0,
    val appLaunchCount: Int = 0,
    val topApps: List<AppUsage> = emptyList(),
    val notificationCount: Int = 0,
    val callCount: Int = 0,
    val unlockCount: Int = 0,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val batteryTemperature: Float = 0f,
)

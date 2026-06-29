package com.lifelog.domain.model

data class AppUsage(
    val id: Long = 0,
    val appName: String,
    val packageName: String,
    val firstOpen: Long,
    val lastOpen: Long,
    val lastClose: Long,
    val totalDuration: Long,
    val launchCount: Int,
    val date: String,
)

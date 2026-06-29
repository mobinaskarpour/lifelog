package com.lifelog.domain.model

data class TimelineSession(
    val packageName: String,
    val appName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
)

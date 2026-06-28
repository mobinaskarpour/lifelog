package com.lifelog.domain.model

data class NotificationLog(
    val id: Long = 0,
    val appName: String,
    val packageName: String,
    val title: String,
    val timestamp: Long,
)

package com.lifelog.domain.model

data class NotificationLog(
    val id: Long = 0,
    val appName: String,
    val packageName: String,
    val title: String,
    val text: String = "",
    val subtext: String = "",
    val bigText: String? = null,
    val notificationId: Int,
    val conversationName: String? = null,
    val timestamp: Long,
    val updatedAt: Long = timestamp,
) {
    val displayBody: String
        get() = bigText?.takeIf { it.isNotBlank() } ?: text.ifBlank { subtext }
}

package com.lifelog.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "timeline_events")
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val packageName: String? = null,
    val colorArgb: Long = 0xFF6200EE,
)

@Entity(
    tableName = "app_usage",
    indices = [Index(value = ["packageName", "date"], unique = true)],
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val firstOpen: Long,
    val lastOpen: Long,
    val lastClose: Long,
    val totalDuration: Long,
    val launchCount: Int,
    val date: String,
)

@Entity(
    tableName = "notification_logs",
    indices = [Index(value = ["packageName", "notificationId"], unique = true)],
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val type: String,
    val duration: Long,
    val timestamp: Long,
)

@Entity(tableName = "location_logs")
data class LocationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
)

@Entity(tableName = "battery_logs")
data class BatteryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: Int,
    val isCharging: Boolean,
    val temperature: Float,
    val isPowerConnected: Boolean,
    val timestamp: Long,
)

@Entity(tableName = "screen_events")
data class ScreenEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val timestamp: Long,
)

@Entity(
    tableName = "sms_logs",
    indices = [
        Index(value = ["providerId"], unique = true),
        Index(value = ["threadId"]),
    ],
)
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerId: Long,
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val body: String,
    val date: Long,
    val dateSent: Long,
    val type: Int,
    val read: Boolean,
    val seen: Boolean,
    val status: Int,
    val subscriptionId: Int,
    val serviceCenter: String?,
    val person: Long?,
)

package com.lifelog.data.datasource

import com.lifelog.database.entity.AppUsageEntity
import com.lifelog.database.entity.BatteryLogEntity
import com.lifelog.database.entity.CallLogEntity
import com.lifelog.database.entity.LocationLogEntity
import com.lifelog.database.entity.NotificationLogEntity
import com.lifelog.database.entity.ScreenEventEntity
import com.lifelog.database.entity.SmsLogEntity
import com.lifelog.database.entity.TimelineEventEntity
import com.lifelog.domain.model.AppUsage
import com.lifelog.domain.model.BatteryLog
import com.lifelog.domain.model.CallLog
import com.lifelog.domain.model.CallType
import com.lifelog.domain.model.LocationLog
import com.lifelog.domain.model.NotificationLog
import com.lifelog.domain.model.ScreenEvent
import com.lifelog.domain.model.ScreenEventType
import com.lifelog.domain.model.SmsMessage
import com.lifelog.domain.model.SmsMessageType
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType

fun TimelineEventEntity.toDomainOrNull(): TimelineEvent? {
    val eventType = TimelineEventType.fromString(type) ?: return null
    return TimelineEvent(
        id = id,
        type = eventType,
        title = title.ifBlank { "Unknown event" },
        subtitle = subtitle,
        timestamp = timestamp.coerceAtLeast(0L),
        packageName = packageName?.takeIf { it.isNotBlank() },
        colorArgb = colorArgb.coerceIn(0L, 0xFFFFFFFFL).takeIf { it != 0L } ?: 0xFF6200EE,
    )
}

fun TimelineEventEntity.toDomain(): TimelineEvent =
    toDomainOrNull()
        ?: TimelineEvent(
            id = id,
            type = TimelineEventType.PHONE_UNLOCKED,
            title = title.ifBlank { "Unknown event" },
            subtitle = subtitle,
            timestamp = timestamp.coerceAtLeast(0L),
            packageName = packageName,
            colorArgb = 0xFF6200EE,
        )

fun TimelineEvent.toEntity() =
    TimelineEventEntity(
        id = id,
        type = type.name,
        title = title,
        subtitle = subtitle,
        timestamp = timestamp,
        packageName = packageName,
        colorArgb = colorArgb,
    )

fun AppUsageEntity.toDomain() =
    AppUsage(
        id = id,
        appName = appName,
        packageName = packageName,
        firstOpen = firstOpen,
        lastOpen = lastOpen,
        lastClose = lastClose,
        totalDuration = totalDuration,
        launchCount = launchCount,
        date = date,
    )

fun AppUsage.toEntity() =
    AppUsageEntity(
        id = id,
        appName = appName,
        packageName = packageName,
        firstOpen = firstOpen,
        lastOpen = lastOpen,
        lastClose = lastClose,
        totalDuration = totalDuration,
        launchCount = launchCount,
        date = date,
    )

fun NotificationLogEntity.toDomain() =
    NotificationLog(
        id = id,
        appName = appName,
        packageName = packageName,
        title = title,
        text = text,
        subtext = subtext,
        bigText = bigText,
        notificationId = notificationId,
        conversationName = conversationName,
        timestamp = timestamp,
        updatedAt = updatedAt,
    )

fun NotificationLog.toEntity() =
    NotificationLogEntity(
        id = id,
        appName = appName,
        packageName = packageName,
        title = title,
        text = text,
        subtext = subtext,
        bigText = bigText,
        notificationId = notificationId,
        conversationName = conversationName,
        timestamp = timestamp,
        updatedAt = updatedAt,
    )

fun CallLogEntity.toDomain() =
    CallLog(
        id = id,
        phoneNumber = phoneNumber,
        contactName = contactName,
        type = CallType.valueOf(type),
        duration = duration,
        timestamp = timestamp,
    )

fun CallLog.toEntity() =
    CallLogEntity(
        id = id,
        phoneNumber = phoneNumber,
        contactName = contactName,
        type = type.name,
        duration = duration,
        timestamp = timestamp,
    )

fun LocationLogEntity.toDomain() =
    LocationLog(
        id = id,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = timestamp,
    )

fun LocationLog.toEntity() =
    LocationLogEntity(
        id = id,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = timestamp,
    )

fun BatteryLogEntity.toDomain() =
    BatteryLog(
        id = id,
        level = level,
        isCharging = isCharging,
        temperature = temperature,
        isPowerConnected = isPowerConnected,
        timestamp = timestamp,
    )

fun BatteryLog.toEntity() =
    BatteryLogEntity(
        id = id,
        level = level,
        isCharging = isCharging,
        temperature = temperature,
        isPowerConnected = isPowerConnected,
        timestamp = timestamp,
    )

fun ScreenEventEntity.toDomain() =
    ScreenEvent(
        id = id,
        type = ScreenEventType.valueOf(type),
        timestamp = timestamp,
    )

fun ScreenEvent.toEntity() =
    ScreenEventEntity(
        id = id,
        type = type.name,
        timestamp = timestamp,
    )

fun SmsLogEntity.toDomain() =
    SmsMessage(
        id = id,
        providerId = providerId,
        threadId = threadId,
        address = address,
        contactName = contactName,
        body = body,
        date = date,
        dateSent = dateSent,
        type = SmsMessageType.fromAndroidType(type),
        read = read,
        seen = seen,
        status = status,
        subscriptionId = subscriptionId,
        serviceCenter = serviceCenter,
        person = person,
    )

fun SmsMessage.toEntity() =
    SmsLogEntity(
        id = id,
        providerId = providerId,
        threadId = threadId,
        address = address,
        contactName = contactName,
        body = body,
        date = date,
        dateSent = dateSent,
        type = type.toAndroidType(),
        read = read,
        seen = seen,
        status = status,
        subscriptionId = subscriptionId,
        serviceCenter = serviceCenter,
        person = person,
    )

private fun SmsMessageType.toAndroidType(): Int =
    when (this) {
        SmsMessageType.INBOX -> 1
        SmsMessageType.SENT -> 2
        SmsMessageType.DRAFT -> 3
        SmsMessageType.OUTBOX -> 4
        SmsMessageType.FAILED -> 5
        SmsMessageType.QUEUED -> 6
        SmsMessageType.UNKNOWN -> 0
    }

package com.lifelog.data.datasource

import com.lifelog.database.entity.AppUsageEntity
import com.lifelog.database.entity.BatteryLogEntity
import com.lifelog.database.entity.CallLogEntity
import com.lifelog.database.entity.LocationLogEntity
import com.lifelog.database.entity.NotificationLogEntity
import com.lifelog.database.entity.ScreenEventEntity
import com.lifelog.database.entity.TimelineEventEntity
import com.lifelog.domain.model.AppUsage
import com.lifelog.domain.model.BatteryLog
import com.lifelog.domain.model.CallLog
import com.lifelog.domain.model.CallType
import com.lifelog.domain.model.LocationLog
import com.lifelog.domain.model.NotificationLog
import com.lifelog.domain.model.ScreenEvent
import com.lifelog.domain.model.ScreenEventType
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType

fun TimelineEventEntity.toDomain() =
    TimelineEvent(
        id = id,
        type = TimelineEventType.valueOf(type),
        title = title,
        subtitle = subtitle,
        timestamp = timestamp,
        packageName = packageName,
        colorArgb = colorArgb,
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
        timestamp = timestamp,
    )

fun NotificationLog.toEntity() =
    NotificationLogEntity(
        id = id,
        appName = appName,
        packageName = packageName,
        title = title,
        timestamp = timestamp,
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

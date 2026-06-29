package com.lifelog.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lifelog.database.dao.AppUsageDao
import com.lifelog.database.dao.BatteryLogDao
import com.lifelog.database.dao.CallLogDao
import com.lifelog.database.dao.LocationLogDao
import com.lifelog.database.dao.NotificationLogDao
import com.lifelog.database.dao.ScreenEventDao
import com.lifelog.database.dao.SmsLogDao
import com.lifelog.database.dao.TimelineEventDao
import com.lifelog.database.entity.AppUsageEntity
import com.lifelog.database.entity.BatteryLogEntity
import com.lifelog.database.entity.CallLogEntity
import com.lifelog.database.entity.LocationLogEntity
import com.lifelog.database.entity.NotificationLogEntity
import com.lifelog.database.entity.ScreenEventEntity
import com.lifelog.database.entity.SmsLogEntity
import com.lifelog.database.entity.TimelineEventEntity

@Database(
    entities = [
        TimelineEventEntity::class,
        AppUsageEntity::class,
        NotificationLogEntity::class,
        CallLogEntity::class,
        LocationLogEntity::class,
        BatteryLogEntity::class,
        ScreenEventEntity::class,
        SmsLogEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class LifeLogDatabase : RoomDatabase() {
    abstract fun timelineEventDao(): TimelineEventDao

    abstract fun appUsageDao(): AppUsageDao

    abstract fun notificationLogDao(): NotificationLogDao

    abstract fun callLogDao(): CallLogDao

    abstract fun locationLogDao(): LocationLogDao

    abstract fun batteryLogDao(): BatteryLogDao

    abstract fun screenEventDao(): ScreenEventDao

    abstract fun smsLogDao(): SmsLogDao

    companion object {
        const val DATABASE_NAME = "lifelog.db"
    }
}

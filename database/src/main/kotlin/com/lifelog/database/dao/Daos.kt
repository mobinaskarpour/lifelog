package com.lifelog.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifelog.database.entity.AppUsageEntity
import com.lifelog.database.entity.BatteryLogEntity
import com.lifelog.database.entity.CallLogEntity
import com.lifelog.database.entity.LocationLogEntity
import com.lifelog.database.entity.NotificationLogEntity
import com.lifelog.database.entity.ScreenEventEntity
import com.lifelog.database.entity.SmsLogEntity
import com.lifelog.database.entity.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {
    @Query("SELECT * FROM timeline_events ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM timeline_events WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun getBetween(
        start: Long,
        end: Long,
    ): Flow<List<TimelineEventEntity>>

    @Query(
        "SELECT * FROM timeline_events WHERE title LIKE '%' || :keyword || '%' " +
            "OR subtitle LIKE '%' || :keyword || '%' ORDER BY timestamp DESC",
    )
    fun search(keyword: String): Flow<List<TimelineEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TimelineEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<TimelineEventEntity>)

    @Query("DELETE FROM timeline_events WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)

    @Query("SELECT * FROM timeline_events")
    suspend fun getAllSync(): List<TimelineEventEntity>
}

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage ORDER BY totalDuration DESC")
    fun getAll(): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY totalDuration DESC")
    fun getForDate(date: String): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date >= :startDate AND date <= :endDate ORDER BY totalDuration DESC")
    fun getBetween(
        startDate: String,
        endDate: String,
    ): Flow<List<AppUsageEntity>>

    @Query(
        """
        SELECT packageName,
               MAX(appName) AS appName,
               MIN(firstOpen) AS firstOpen,
               MAX(lastOpen) AS lastOpen,
               MAX(lastClose) AS lastClose,
               SUM(totalDuration) AS totalDuration,
               SUM(launchCount) AS launchCount,
               MAX(date) AS date,
               MIN(id) AS id
        FROM app_usage
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY packageName
        ORDER BY totalDuration DESC
        """,
    )
    fun getAggregatedBetween(
        startDate: String,
        endDate: String,
    ): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getByPackageAndDate(
        packageName: String,
        date: String,
    ): AppUsageEntity?

    @Query("SELECT * FROM app_usage ORDER BY totalDuration DESC LIMIT :limit")
    fun getTop(limit: Int): Flow<List<AppUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: AppUsageEntity)

    @Query("DELETE FROM app_usage WHERE date < :beforeDate")
    suspend fun deleteBefore(beforeDate: String)

    @Query("SELECT * FROM app_usage")
    suspend fun getAllSync(): List<AppUsageEntity>
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_logs WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun getForDate(
        start: Long,
        end: Long,
    ): Flow<List<NotificationLogEntity>>

    @Query("SELECT COUNT(*) FROM notification_logs WHERE timestamp >= :start AND timestamp < :end")
    fun getCountForDate(
        start: Long,
        end: Long,
    ): Flow<Int>

    @Query(
        "SELECT * FROM notification_logs WHERE title LIKE '%' || :keyword || '%' " +
            "OR appName LIKE '%' || :keyword || '%' " +
            "OR text LIKE '%' || :keyword || '%' " +
            "OR subtext LIKE '%' || :keyword || '%' " +
            "OR bigText LIKE '%' || :keyword || '%' " +
            "OR conversationName LIKE '%' || :keyword || '%' " +
            "OR packageName LIKE '%' || :keyword || '%' ORDER BY timestamp DESC",
    )
    fun search(keyword: String): Flow<List<NotificationLogEntity>>

    @Query(
        "SELECT * FROM notification_logs WHERE packageName = :packageName ORDER BY timestamp DESC",
    )
    fun getByPackage(packageName: String): Flow<List<NotificationLogEntity>>

    @Query(
        "SELECT * FROM notification_logs WHERE packageName = :packageName AND notificationId = :notificationId LIMIT 1",
    )
    suspend fun findByPackageAndNotificationId(
        packageName: String,
        notificationId: Int,
    ): NotificationLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationLogEntity): Long

    @Query("DELETE FROM notification_logs WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)

    @Query("SELECT * FROM notification_logs")
    suspend fun getAllSync(): List<NotificationLogEntity>
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun getForDate(
        start: Long,
        end: Long,
    ): Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_logs WHERE timestamp >= :start AND timestamp < :end")
    fun getCountForDate(
        start: Long,
        end: Long,
    ): Flow<Int>

    @Query(
        "SELECT * FROM call_logs WHERE phoneNumber LIKE '%' || :keyword || '%' " +
            "OR contactName LIKE '%' || :keyword || '%' ORDER BY timestamp DESC",
    )
    fun search(keyword: String): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: CallLogEntity): Long

    @Query("DELETE FROM call_logs WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)

    @Query("SELECT * FROM call_logs")
    suspend fun getAllSync(): List<CallLogEntity>
}

@Dao
interface LocationLogDao {
    @Query("SELECT * FROM location_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<LocationLogEntity>>

    @Query("SELECT * FROM location_logs WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getBetween(
        start: Long,
        end: Long,
    ): Flow<List<LocationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationLogEntity): Long

    @Query("DELETE FROM location_logs WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)

    @Query("SELECT * FROM location_logs")
    suspend fun getAllSync(): List<LocationLogEntity>
}

@Dao
interface BatteryLogDao {
    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BatteryLogEntity>>

    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): Flow<BatteryLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: BatteryLogEntity): Long

    @Query("DELETE FROM battery_logs WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)

    @Query("SELECT * FROM battery_logs")
    suspend fun getAllSync(): List<BatteryLogEntity>
}

@Dao
interface ScreenEventDao {
    @Query("SELECT * FROM screen_events ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ScreenEventEntity>>

    @Query("SELECT COUNT(*) FROM screen_events WHERE type = 'DEVICE_UNLOCK' AND timestamp >= :start AND timestamp < :end")
    fun getUnlockCountForDate(
        start: Long,
        end: Long,
    ): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ScreenEventEntity): Long

    @Query("DELETE FROM screen_events WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)

    @Query("SELECT * FROM screen_events")
    suspend fun getAllSync(): List<ScreenEventEntity>
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY date DESC")
    fun getAll(): Flow<List<SmsLogEntity>>

    @Query("SELECT * FROM sms_logs WHERE threadId = :threadId ORDER BY date ASC")
    fun getForThread(threadId: Long): Flow<List<SmsLogEntity>>

    @Query(
        "SELECT * FROM sms_logs WHERE body LIKE '%' || :keyword || '%' " +
            "OR address LIKE '%' || :keyword || '%' " +
            "OR contactName LIKE '%' || :keyword || '%' ORDER BY date DESC",
    )
    fun search(keyword: String): Flow<List<SmsLogEntity>>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE type = :type")
    suspend fun getCountByType(type: Int): Int

    @Query("SELECT COUNT(*) FROM sms_logs WHERE type = :type")
    fun observeCountByType(type: Int): Flow<Int>

    @Query("SELECT * FROM sms_logs WHERE providerId = :providerId LIMIT 1")
    suspend fun findByProviderId(providerId: Long): SmsLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SmsLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<SmsLogEntity>)

    @Query("DELETE FROM sms_logs WHERE date < :before")
    suspend fun deleteBefore(before: Long)

    @Query("SELECT * FROM sms_logs")
    suspend fun getAllSync(): List<SmsLogEntity>
}

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
            "OR appName LIKE '%' || :keyword || '%' ORDER BY timestamp DESC",
    )
    fun search(keyword: String): Flow<List<NotificationLogEntity>>

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

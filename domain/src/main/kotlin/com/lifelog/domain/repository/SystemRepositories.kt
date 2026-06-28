package com.lifelog.domain.repository

import com.lifelog.domain.model.BatteryLog
import com.lifelog.domain.model.LocationLog
import com.lifelog.domain.model.ScreenEvent
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getAllLocations(): Flow<List<LocationLog>>

    fun getLocationsBetween(
        start: Long,
        end: Long,
    ): Flow<List<LocationLog>>

    suspend fun insertLocation(location: LocationLog): Long

    suspend fun deleteOldLocations(beforeTimestamp: Long)
}

interface BatteryRepository {
    fun getAllBatteryLogs(): Flow<List<BatteryLog>>

    fun getLatestBatteryLog(): Flow<BatteryLog?>

    suspend fun insertBatteryLog(log: BatteryLog): Long

    suspend fun deleteOldBatteryLogs(beforeTimestamp: Long)
}

interface ScreenEventRepository {
    fun getAllScreenEvents(): Flow<List<ScreenEvent>>

    fun getUnlockCountForDate(date: Long): Flow<Int>

    suspend fun insertScreenEvent(event: ScreenEvent): Long

    suspend fun deleteOldScreenEvents(beforeTimestamp: Long)
}

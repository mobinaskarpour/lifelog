package com.lifelog.domain.repository

import com.lifelog.domain.model.AppUsage
import com.lifelog.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow

interface TimelineRepository {
    fun getAllEvents(): Flow<List<TimelineEvent>>

    fun getEventsForDate(date: Long): Flow<List<TimelineEvent>>

    fun getEventsBetween(
        start: Long,
        end: Long,
    ): Flow<List<TimelineEvent>>

    fun searchEvents(keyword: String): Flow<List<TimelineEvent>>

    suspend fun insertEvent(event: TimelineEvent): Long

    suspend fun insertEvents(events: List<TimelineEvent>)

    suspend fun deleteOldEvents(beforeTimestamp: Long)
}

interface AppUsageRepository {
    fun getAllUsage(): Flow<List<AppUsage>>

    fun getUsageForDate(date: String): Flow<List<AppUsage>>

    fun getUsageBetween(
        startDate: String,
        endDate: String,
    ): Flow<List<AppUsage>>

    fun getAggregatedUsageBetween(
        startDate: String,
        endDate: String,
    ): Flow<List<AppUsage>>

    fun getTopApps(limit: Int): Flow<List<AppUsage>>

    suspend fun insertOrUpdateUsage(usage: AppUsage)

    suspend fun deleteOldUsage(beforeDate: String)
}

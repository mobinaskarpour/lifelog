package com.lifelog.domain.repository

import com.lifelog.domain.model.CallLog
import com.lifelog.domain.model.NotificationLog
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<NotificationLog>>

    fun getNotificationsForDate(date: Long): Flow<List<NotificationLog>>

    fun searchNotifications(keyword: String): Flow<List<NotificationLog>>

    fun getNotificationCountForDate(date: Long): Flow<Int>

    suspend fun insertNotification(notification: NotificationLog): Long

    suspend fun deleteOldNotifications(beforeTimestamp: Long)
}

interface CallRepository {
    fun getAllCalls(): Flow<List<CallLog>>

    fun getCallsForDate(date: Long): Flow<List<CallLog>>

    fun searchCalls(keyword: String): Flow<List<CallLog>>

    fun getCallCountForDate(date: Long): Flow<Int>

    suspend fun insertCall(call: CallLog): Long

    suspend fun deleteOldCalls(beforeTimestamp: Long)
}

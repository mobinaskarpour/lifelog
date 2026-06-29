package com.lifelog.domain.repository

import com.lifelog.domain.model.CallLog
import com.lifelog.domain.model.NotificationLog
import com.lifelog.domain.model.SmsMessage
import com.lifelog.domain.model.SmsMessageType
import com.lifelog.domain.model.SmsSyncStats
import com.lifelog.domain.model.SmsThread
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<NotificationLog>>

    fun getNotificationsForDate(date: Long): Flow<List<NotificationLog>>

    fun searchNotifications(keyword: String): Flow<List<NotificationLog>>

    fun getNotificationsByPackage(packageName: String): Flow<List<NotificationLog>>

    fun getNotificationCountForDate(date: Long): Flow<Int>

    suspend fun upsertNotification(notification: NotificationLog): Long

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

interface SmsRepository {
    fun getAllMessages(): Flow<List<SmsMessage>>

    fun getMessagesForThread(threadId: Long): Flow<List<SmsMessage>>

    fun getAllThreads(): Flow<List<SmsThread>>

    fun getSyncStats(): Flow<SmsSyncStats>

    fun searchMessages(keyword: String): Flow<List<SmsMessage>>

    suspend fun upsertMessage(message: SmsMessage): Long

    suspend fun upsertMessages(messages: List<SmsMessage>)

    suspend fun deleteOldMessages(beforeTimestamp: Long)

    suspend fun getCountByType(type: SmsMessageType): Int

    suspend fun updateProviderStats(stats: SmsSyncStats)
}

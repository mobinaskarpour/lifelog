package com.lifelog.domain.repository

import com.lifelog.domain.model.UnifiedMessage
import kotlinx.coroutines.flow.Flow

interface UnifiedMessageRepository {
    fun getAllMessages(): Flow<List<UnifiedMessage>>

    fun getMessagesBySource(source: String): Flow<List<UnifiedMessage>>

    fun getMessagesForConversation(
        source: String,
        sender: String,
    ): Flow<List<UnifiedMessage>>

    fun searchMessages(keyword: String): Flow<List<UnifiedMessage>>

    suspend fun upsertMessage(message: UnifiedMessage): Long

    suspend fun deleteOldMessages(beforeTimestamp: Long)
}

package com.lifelog.domain.repository

import com.lifelog.domain.model.AccessibilityDebugEvent
import com.lifelog.domain.model.MessageChannel
import com.lifelog.domain.model.MessagesOverviewStats
import com.lifelog.domain.model.UniversalChatMessage
import com.lifelog.domain.model.UniversalConversation
import kotlinx.coroutines.flow.Flow

interface MessagesRepository {
    fun observeConversations(
        channel: MessageChannel,
        searchQuery: String,
    ): Flow<List<UniversalConversation>>

    fun observeStats(): Flow<MessagesOverviewStats>

    fun observeMessages(conversationId: String): Flow<List<UniversalChatMessage>>
}

interface AccessibilityDebugRepository {
    fun observeDebugEvents(): Flow<List<AccessibilityDebugEvent>>

    suspend fun clearDebugEvents()
}

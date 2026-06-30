package com.lifelog.data.repository

import com.lifelog.domain.model.MessageChannel
import com.lifelog.domain.model.MessagesOverviewStats
import com.lifelog.domain.model.UniversalChatMessage
import com.lifelog.domain.model.UniversalConversation
import com.lifelog.domain.repository.MessagesRepository
import com.lifelog.domain.repository.SmsRepository
import com.lifelog.domain.repository.UnifiedMessageRepository
import com.lifelog.domain.util.ConversationId
import com.lifelog.domain.util.UniversalConversationGrouper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagesRepositoryImpl
    @Inject
    constructor(
        private val unifiedMessageRepository: UnifiedMessageRepository,
        private val smsRepository: SmsRepository,
    ) : MessagesRepository {
        override fun observeConversations(
            channel: MessageChannel,
            searchQuery: String,
        ): Flow<List<UniversalConversation>> =
            combine(
                unifiedMessageRepository.getAllMessages(),
                smsRepository.getAllThreads(),
            ) { appMessages, smsThreads ->
                val appConversations = UniversalConversationGrouper.fromAppMessages(appMessages)
                val smsConversations = UniversalConversationGrouper.fromSmsThreads(smsThreads)
                val merged = UniversalConversationGrouper.mergeConversations(appConversations, smsConversations)
                val filtered = UniversalConversationGrouper.filterByChannel(merged, channel)
                UniversalConversationGrouper.filterBySearch(filtered, searchQuery)
            }.distinctUntilChanged()

        override fun observeStats(): Flow<MessagesOverviewStats> =
            combine(
                unifiedMessageRepository.getAllMessages(),
                smsRepository.getAllMessages(),
                smsRepository.getAllThreads(),
            ) { appMessages, smsMessages, smsThreads ->
                val appConversations = UniversalConversationGrouper.fromAppMessages(appMessages)
                val smsConversations = UniversalConversationGrouper.fromSmsThreads(smsThreads)
                val merged = UniversalConversationGrouper.mergeConversations(appConversations, smsConversations)
                UniversalConversationGrouper.computeStats(merged, appMessages, smsMessages)
            }.distinctUntilChanged()

        override fun observeMessages(conversationId: String): Flow<List<UniversalChatMessage>> {
            ConversationId.parseSms(conversationId)?.let { threadId ->
                return smsRepository.getMessagesForThread(threadId)
                    .map { messages -> UniversalConversationGrouper.toChatMessagesFromSms(messages) }
            }

            val appConversation = ConversationId.parseApp(conversationId) ?: return flowOf(emptyList())
            val (source, sender) = appConversation
            return unifiedMessageRepository.getMessagesForConversation(source, sender)
                .map { messages -> UniversalConversationGrouper.toChatMessagesFromApp(messages) }
        }
    }

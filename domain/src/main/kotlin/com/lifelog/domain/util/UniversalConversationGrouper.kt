package com.lifelog.domain.util

import com.lifelog.domain.model.MessageChannel
import com.lifelog.domain.model.MessagesOverviewStats
import com.lifelog.domain.model.SmsMessage
import com.lifelog.domain.model.SmsThread
import com.lifelog.domain.model.UnifiedMessage
import com.lifelog.domain.model.UniversalChatMessage
import com.lifelog.domain.model.UniversalConversation

object UniversalConversationGrouper {
    fun fromAppMessages(messages: List<UnifiedMessage>): List<UniversalConversation> =
        messages
            .groupBy { "${it.source}:${it.sender}" }
            .mapNotNull { (_, threadMessages) ->
                val latest = threadMessages.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                val channel = MessageChannel.fromSourceString(latest.source)
                UniversalConversation(
                    id = ConversationId.forApp(latest.source, latest.sender),
                    channel = channel,
                    packageName = latest.packageName,
                    displayName = latest.sender,
                    lastMessage = latest.text,
                    lastTimestamp = latest.timestamp,
                    messageCount = threadMessages.size,
                    unreadCount = 0,
                    isLastOutgoing = false,
                )
            }

    fun fromSmsThreads(threads: List<SmsThread>): List<UniversalConversation> =
        threads.map { thread ->
            UniversalConversation(
                id = ConversationId.forSms(thread.threadId),
                channel = MessageChannel.SMS,
                packageName = null,
                displayName = thread.contactName ?: thread.address,
                lastMessage = thread.lastMessage,
                lastTimestamp = thread.lastDate,
                messageCount = thread.messageCount,
                unreadCount = thread.unreadCount,
                isLastOutgoing = thread.isLastOutgoing,
            )
        }

    fun mergeConversations(
        appConversations: List<UniversalConversation>,
        smsConversations: List<UniversalConversation>,
    ): List<UniversalConversation> = (appConversations + smsConversations).sortedByDescending { it.lastTimestamp }

    fun filterByChannel(
        conversations: List<UniversalConversation>,
        channel: MessageChannel,
    ): List<UniversalConversation> =
        when (channel) {
            MessageChannel.ALL -> conversations
            else -> conversations.filter { it.channel == channel }
        }

    fun filterBySearch(
        conversations: List<UniversalConversation>,
        query: String,
    ): List<UniversalConversation> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return conversations
        return conversations.filter { conversation ->
            conversation.displayName.lowercase().contains(normalized) ||
                conversation.lastMessage.lowercase().contains(normalized) ||
                conversation.channel.displayName.lowercase().contains(normalized)
        }
    }

    fun toChatMessagesFromApp(messages: List<UnifiedMessage>): List<UniversalChatMessage> =
        messages.map { message ->
            UniversalChatMessage(
                id = "app:${message.id}",
                channel = MessageChannel.fromSourceString(message.source),
                sender = message.sender,
                text = message.text,
                timestamp = message.timestamp,
                isOutgoing = false,
                packageName = message.packageName,
            )
        }

    fun toChatMessagesFromSms(messages: List<SmsMessage>): List<UniversalChatMessage> =
        messages.map { message ->
            UniversalChatMessage(
                id = "sms:${message.providerId}",
                channel = MessageChannel.SMS,
                sender = message.contactName ?: message.address,
                text = message.body,
                timestamp = message.date,
                isOutgoing = message.type.isOutgoing,
                packageName = null,
            )
        }

    fun computeStats(
        conversations: List<UniversalConversation>,
        appMessages: List<UnifiedMessage>,
        smsMessages: List<SmsMessage>,
    ): MessagesOverviewStats {
        val telegram = appMessages.count { it.source.equals("telegram", ignoreCase = true) }
        val whatsapp = appMessages.count { it.source.equals("whatsapp", ignoreCase = true) }
        val instagram = appMessages.count { it.source.equals("instagram", ignoreCase = true) }
        val sms = smsMessages.size
        return MessagesOverviewStats(
            totalConversations = conversations.size,
            totalMessages = appMessages.size + sms,
            telegramCount = telegram,
            whatsappCount = whatsapp,
            instagramCount = instagram,
            smsCount = sms,
        )
    }

    fun matchesSearch(
        message: UniversalChatMessage,
        query: String,
    ): Boolean {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return true
        return message.sender.lowercase().contains(normalized) ||
            message.text.lowercase().contains(normalized) ||
            message.channel.displayName.lowercase().contains(normalized)
    }
}

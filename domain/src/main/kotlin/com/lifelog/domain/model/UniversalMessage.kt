package com.lifelog.domain.model

enum class MessageChannel {
    ALL,
    TELEGRAM,
    WHATSAPP,
    INSTAGRAM,
    SMS,
    ;

    val displayName: String
        get() =
            when (this) {
                ALL -> "All"
                TELEGRAM -> "Telegram"
                WHATSAPP -> "WhatsApp"
                INSTAGRAM -> "Instagram"
                SMS -> "SMS"
            }

    val packageName: String?
        get() =
            when (this) {
                TELEGRAM -> "org.telegram.messenger"
                WHATSAPP -> "com.whatsapp"
                INSTAGRAM -> "com.instagram.android"
                else -> null
            }

    companion object {
        fun fromSourceString(source: String): MessageChannel =
            when (source.lowercase()) {
                "telegram" -> TELEGRAM
                "whatsapp" -> WHATSAPP
                "instagram" -> INSTAGRAM
                "sms" -> SMS
                else -> ALL
            }
    }
}

data class UniversalConversation(
    val id: String,
    val channel: MessageChannel,
    val packageName: String?,
    val displayName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val messageCount: Int,
    val unreadCount: Int,
    val isLastOutgoing: Boolean,
)

data class UniversalChatMessage(
    val id: String,
    val channel: MessageChannel,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val packageName: String?,
)

data class MessagesOverviewStats(
    val totalConversations: Int = 0,
    val totalMessages: Int = 0,
    val telegramCount: Int = 0,
    val whatsappCount: Int = 0,
    val instagramCount: Int = 0,
    val smsCount: Int = 0,
)

data class AccessibilityDebugEvent(
    val id: Long,
    val timestamp: Long,
    val eventType: String,
    val packageName: String,
    val parsedSender: String?,
    val parsedMessage: String?,
    val nodeCount: Int,
    val rawNodesPreview: String,
    val confidence: Float?,
)

package com.lifelog.domain.model

enum class SmsMessageType {
    INBOX,
    SENT,
    OUTBOX,
    DRAFT,
    FAILED,
    QUEUED,
    UNKNOWN,
    ;

    val isOutgoing: Boolean
        get() = this in setOf(SENT, OUTBOX, QUEUED)

    companion object {
        fun fromAndroidType(type: Int): SmsMessageType =
            when (type) {
                1 -> INBOX
                2 -> SENT
                3 -> DRAFT
                4 -> OUTBOX
                5 -> FAILED
                6 -> QUEUED
                else -> UNKNOWN
            }
    }
}

data class SmsMessage(
    val id: Long = 0,
    val providerId: Long,
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val body: String,
    val date: Long,
    val dateSent: Long,
    val type: SmsMessageType,
    val read: Boolean,
    val seen: Boolean,
    val status: Int,
    val subscriptionId: Int,
    val serviceCenter: String?,
    val person: Long?,
)

data class SmsThread(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val lastMessage: String,
    val lastDate: Long,
    val messageCount: Int,
    val unreadCount: Int = 0,
    val isLastOutgoing: Boolean = false,
)

data class SmsSyncStats(
    val inboxCount: Int = 0,
    val sentCount: Int = 0,
    val outboxCount: Int = 0,
    val draftCount: Int = 0,
    val failedCount: Int = 0,
    val queuedCount: Int = 0,
    val providerInboxCount: Int = 0,
    val providerSentCount: Int = 0,
    val providerOutboxCount: Int = 0,
    val providerDraftCount: Int = 0,
)

enum class SmsAccessStatus {
    GRANTED,
    PERMISSION_DENIED,
    PROVIDER_RESTRICTED,
    PROVIDER_UNAVAILABLE,
}

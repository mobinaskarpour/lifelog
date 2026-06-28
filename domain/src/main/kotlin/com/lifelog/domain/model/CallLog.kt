package com.lifelog.domain.model

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
}

data class CallLog(
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val type: CallType,
    val duration: Long,
    val timestamp: Long,
)

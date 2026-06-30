package com.lifelog.domain.model

data class UnifiedMessage(
    val id: Long = 0,
    val source: String,
    val packageName: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val capturedAt: Long,
    val rawNodesJson: String?,
    val dedupKey: String,
)

package com.lifelog.service.accessibility

data class UiTextNode(
    val text: String,
    val className: String,
    val depth: Int,
    val top: Int,
    val bottom: Int,
    val isEditable: Boolean,
)

data class ExtractedMessage(
    val source: String,
    val packageName: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val rawNodes: List<UiTextNode>,
)

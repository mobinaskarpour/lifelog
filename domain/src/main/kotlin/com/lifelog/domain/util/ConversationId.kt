package com.lifelog.domain.util

import java.net.URLDecoder
import java.net.URLEncoder

object ConversationId {
    private const val CHARSET = "UTF-8"

    fun forSms(threadId: Long): String = "sms:$threadId"

    fun forApp(
        source: String,
        sender: String,
    ): String = "app:$source:${encode(sender)}"

    fun parseSms(conversationId: String): Long? {
        if (!conversationId.startsWith("sms:")) return null
        return conversationId.removePrefix("sms:").toLongOrNull()
    }

    fun parseApp(conversationId: String): Pair<String, String>? {
        if (!conversationId.startsWith("app:")) return null
        val body = conversationId.removePrefix("app:")
        val separator = body.indexOf(':')
        if (separator <= 0 || separator >= body.lastIndex) return null
        val source = body.substring(0, separator)
        val sender = decode(body.substring(separator + 1))
        return source to sender
    }

    private fun encode(value: String): String = URLEncoder.encode(value, CHARSET)

    private fun decode(value: String): String = URLDecoder.decode(value, CHARSET)
}

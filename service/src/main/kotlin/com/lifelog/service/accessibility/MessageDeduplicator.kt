package com.lifelog.service.accessibility

import java.security.MessageDigest

class MessageDeduplicator {
    private val recentKeys = LinkedHashMap<String, Long>()

    fun shouldIngest(message: ExtractedMessage): Boolean {
        val key = buildDedupKey(message)
        val now = System.currentTimeMillis()
        val lastSeen = recentKeys[key]
        if (lastSeen != null && now - lastSeen < DEDUP_WINDOW_MS) {
            return false
        }
        recentKeys[key] = now
        if (recentKeys.size > MAX_CACHE_SIZE) {
            recentKeys.entries.firstOrNull()?.let { recentKeys.remove(it.key) }
        }
        return true
    }

    fun buildDedupKey(message: ExtractedMessage): String {
        val timeBucket = message.timestamp / DEDUP_WINDOW_MS
        val raw =
            "${message.source}|${message.sender}|${message.text}|" +
                "${message.packageName}|$timeBucket"
        return sha256(raw)
    }

    fun clear() {
        recentKeys.clear()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DEDUP_WINDOW_MS = 5_000L
        private const val MAX_CACHE_SIZE = 500
    }
}

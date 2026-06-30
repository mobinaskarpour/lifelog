package com.lifelog.service.accessibility

object MessageExtractor {
    private val IGNORED_TEXT =
        setOf(
            "send",
            "back",
            "search",
            "menu",
            "more",
            "call",
            "video",
            "mute",
            "attach",
            "camera",
            "emoji",
            "voice message",
            "type a message",
            "message",
            "messages",
            "chats",
            "chat",
            "online",
            "last seen",
            "typing",
            "reply",
            "forward",
            "delete",
            "copy",
            "share",
            "info",
            "media",
            "links",
            "docs",
            "stickers",
            "gif",
            "sticker",
            "new chat",
            "archived",
            "status",
            "calls",
            "settings",
            "contacts",
            "create",
            "edit",
            "done",
            "cancel",
            "ok",
            "yes",
            "no",
            "today",
            "yesterday",
        )

    private val TIMESTAMP_REGEX =
        Regex(
            """^(\d{1,2}:\d{2}(\s?[AP]M)?|\d{1,2}/\d{1,2}/\d{2,4}|Today|Yesterday)$""",
            RegexOption.IGNORE_CASE,
        )

    private val chatSnapshots = mutableMapOf<String, Set<String>>()

    fun packageToSource(packageName: String): String =
        when (packageName) {
            "org.telegram.messenger" -> "telegram"
            "com.whatsapp" -> "whatsapp"
            "com.instagram.android" -> "instagram"
            else -> packageName.substringAfterLast('.')
        }

    fun extractNewMessages(
        packageName: String,
        nodes: List<UiTextNode>,
        chatSender: String?,
    ): List<ExtractedMessage> {
        val sender = chatSender?.takeIf { it.isNotBlank() } ?: inferSender(nodes) ?: "Unknown"
        val candidates = findMessageCandidates(nodes)
        if (candidates.isEmpty()) return emptyList()

        val snapshotKey = "$packageName:$sender"
        val previous = chatSnapshots[snapshotKey] ?: emptySet()
        val currentTexts = candidates.map { it.text }.toSet()
        val newTexts = currentTexts - previous
        chatSnapshots[snapshotKey] = currentTexts
        if (chatSnapshots.size > MAX_SNAPSHOTS) {
            chatSnapshots.keys.firstOrNull()?.let { chatSnapshots.remove(it) }
        }

        if (newTexts.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val source = packageToSource(packageName)
        return candidates
            .filter { it.text in newTexts }
            .map { node ->
                ExtractedMessage(
                    source = source,
                    packageName = packageName,
                    sender = sender,
                    text = node.text,
                    timestamp = now,
                    rawNodes = listOf(node),
                )
            }
    }

    fun inferSender(nodes: List<UiTextNode>): String? {
        val topCandidates =
            nodes
                .filter { node ->
                    !UiTreeParser.isInputField(node) &&
                        !isNoise(node.text) &&
                        node.text.length in 2..60
                }
                .sortedBy { it.top }
                .take(8)
        return topCandidates.firstOrNull()?.text
    }

    private fun findMessageCandidates(nodes: List<UiTextNode>): List<UiTextNode> {
        val screenMid =
            nodes
                .map { (it.top + it.bottom) / 2 }
                .sorted()
                .let { coords ->
                    if (coords.isEmpty()) 0 else coords[coords.size / 2]
                }

        return nodes
            .filter { node ->
                !UiTreeParser.isInputField(node) &&
                    !isNoise(node.text) &&
                    node.text.length >= 2 &&
                    (node.top + node.bottom) / 2 >= screenMid - 200
            }
            .distinctBy { it.text }
            .sortedBy { it.top }
    }

    private fun isNoise(text: String): Boolean {
        val normalized = text.trim().lowercase()
        if (normalized.length <= 1) return true
        if (normalized in IGNORED_TEXT) return true
        if (TIMESTAMP_REGEX.matches(normalized)) return true
        if (normalized.all { it.isDigit() || it == ':' || it == ' ' }) return true
        return false
    }

    fun clearSnapshots() {
        chatSnapshots.clear()
    }

    private const val MAX_SNAPSHOTS = 50
}

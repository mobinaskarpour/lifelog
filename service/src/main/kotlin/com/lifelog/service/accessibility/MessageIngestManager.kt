package com.lifelog.service.accessibility

import com.lifelog.domain.model.UnifiedMessage
import com.lifelog.domain.repository.UnifiedMessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageIngestManager
    @Inject
    constructor(
        private val unifiedMessageRepository: UnifiedMessageRepository,
        private val deduplicator: MessageDeduplicator,
    ) {
        fun ingest(
            scope: CoroutineScope,
            message: ExtractedMessage,
        ) {
            if (!deduplicator.shouldIngest(message)) {
                Timber.tag(TAG).d("Skipped duplicate: ${message.sender}: ${message.text.take(40)}")
                return
            }

            val capturedAt = System.currentTimeMillis()
            val unified =
                UnifiedMessage(
                    source = message.source,
                    packageName = message.packageName,
                    sender = message.sender,
                    text = message.text,
                    timestamp = message.timestamp,
                    capturedAt = capturedAt,
                    rawNodesJson = encodeRawNodes(message.rawNodes),
                    dedupKey = deduplicator.buildDedupKey(message),
                )

            scope.launch {
                try {
                    unifiedMessageRepository.upsertMessage(unified)
                    Timber.tag(TAG).d(
                        "Ingested ${message.source} message from ${message.sender}: " +
                            message.text.take(60),
                    )
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to ingest message")
                }
            }
        }

        private fun encodeRawNodes(nodes: List<UiTextNode>): String =
            nodes.joinToString(separator = ",") { node ->
                """{"text":${jsonString(node.text)},"class":${jsonString(node.className)}}"""
            }.let { "[$it]" }

        private fun jsonString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

        companion object {
            private const val TAG = "Accessibility"
        }
    }

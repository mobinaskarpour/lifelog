package com.lifelog.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.lifelog.domain.model.AccessibilityDebugEvent
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.repository.SettingsRepository
import com.lifelog.domain.repository.TimelineRepository
import com.lifelog.service.accessibility.AccessibilityDebugStore
import com.lifelog.service.accessibility.MessageExtractor
import com.lifelog.service.accessibility.MessageIngestManager
import com.lifelog.service.accessibility.UiTreeParser
import com.lifelog.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
open class MessageAccessibilityService : AccessibilityService() {
    @Inject lateinit var timelineRepository: TimelineRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var messageIngestManager: MessageIngestManager

    @Inject lateinit var accessibilityDebugStore: AccessibilityDebugStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentEventKeys = LinkedHashMap<String, Long>()
    private var lastForegroundPackage: String? = null
    private var currentChatSender: String? = null
    private var messageCaptureEnabled = true
    private var processJob: Job? = null
    private var lastTreeHash: Int? = null
    private var lastProcessedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.tag(TAG).d("Message accessibility service connected")
        scope.launch {
            settingsRepository.getSettings().collect { settings ->
                messageCaptureEnabled = settings.appMessageCaptureEnabled
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowChange(event)
                if (isMessagingPackage(packageName)) {
                    currentChatSender = null
                    lastTreeHash = null
                    scheduleMessageProcessing(packageName)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            -> {
                if (isMessagingPackage(packageName)) {
                    recordDebugEvent(
                        eventType = eventTypeName(event.eventType),
                        packageName = packageName,
                        parsedSender = currentChatSender,
                        parsedMessage = event.text?.toString(),
                        nodeCount = 0,
                        rawNodesPreview = event.text?.toString().orEmpty(),
                        confidence = null,
                    )
                    scheduleMessageProcessing(packageName)
                }
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED ->
                logInteraction(event, TimelineEventType.VIEW_CLICKED, "Tap")
            AccessibilityEvent.TYPE_VIEW_SCROLLED ->
                logInteraction(event, TimelineEventType.VIEW_SCROLLED, "Scroll")
        }
    }

    override fun onInterrupt() {
        Timber.tag(TAG).d("Message accessibility service interrupted")
    }

    private fun scheduleMessageProcessing(packageName: String) {
        if (!messageCaptureEnabled) return
        processJob?.cancel()
        processJob =
            scope.launch {
                delay(PROCESS_DEBOUNCE_MS)
                processActiveWindow(packageName)
            }
    }

    private fun processActiveWindow(packageName: String) {
        if (!messageCaptureEnabled || !isMessagingPackage(packageName)) return

        val root = rootInActiveWindow
        if (root == null) {
            Timber.tag(TAG).d("No active window root for $packageName")
            return
        }

        try {
            val nodes = UiTreeParser.parse(root)
            val treeHash = UiTreeParser.contentHash(nodes)
            if (treeHash == lastTreeHash && packageName == lastProcessedPackage) {
                return
            }
            lastTreeHash = treeHash
            lastProcessedPackage = packageName

            if (currentChatSender == null) {
                currentChatSender = MessageExtractor.inferSender(nodes)
            }

            val messages =
                MessageExtractor.extractNewMessages(
                    packageName = packageName,
                    nodes = nodes,
                    chatSender = currentChatSender,
                )

            if (messages.isEmpty()) {
                recordDebugEvent(
                    eventType = "TREE_PARSED",
                    packageName = packageName,
                    parsedSender = currentChatSender,
                    parsedMessage = null,
                    nodeCount = nodes.size,
                    rawNodesPreview = nodes.take(6).joinToString(" | ") { it.text.take(40) },
                    confidence = 0.4f,
                )
                Timber.tag(TAG).d("No new messages detected in $packageName (${nodes.size} nodes)")
                return
            }

            messages.forEach { message ->
                recordDebugEvent(
                    eventType = "MESSAGE_EXTRACTED",
                    packageName = packageName,
                    parsedSender = message.sender,
                    parsedMessage = message.text,
                    nodeCount = nodes.size,
                    rawNodesPreview = message.rawNodes.joinToString(" | ") { it.text.take(40) },
                    confidence = 0.85f,
                )
                messageIngestManager.ingest(scope, message)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error processing accessibility tree for $packageName")
        } finally {
            root.recycle()
        }
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (!AppUtils.shouldTrackPackage(packageName) || packageName == this.packageName) return
        if (packageName == lastForegroundPackage) return

        val previous = lastForegroundPackage
        if (previous != null && AppUtils.shouldTrackPackage(previous)) {
            logAppEvent(previous, TimelineEventType.APP_CLOSED, event.eventTime)
        }

        lastForegroundPackage = packageName
        logAppEvent(packageName, TimelineEventType.APP_OPENED, event.eventTime)
        logWindowChange(packageName, event.eventTime)
    }

    private fun logWindowChange(
        packageName: String,
        timestamp: Long,
    ) {
        val key = "WINDOW:$packageName"
        if (!shouldLog(key, timestamp)) return
        val appName = AppUtils.getAppName(this, packageName)
        logTimeline(
            type = TimelineEventType.WINDOW_CHANGED,
            title = appName,
            subtitle = "Window changed",
            timestamp = timestamp,
            packageName = packageName,
            colorArgb = 0xFF3F51B5,
        )
    }

    private fun logInteraction(
        event: AccessibilityEvent,
        type: TimelineEventType,
        label: String,
    ) {
        val packageName = event.packageName?.toString() ?: return
        if (!AppUtils.shouldTrackPackage(packageName)) return
        val key = "${type.name}:$packageName"
        if (!shouldLog(key, event.eventTime)) return
        val appName = AppUtils.getAppName(this, packageName)
        logTimeline(
            type = type,
            title = "$appName $label",
            subtitle = event.className?.toString().orEmpty(),
            timestamp = event.eventTime,
            packageName = packageName,
            colorArgb = if (type == TimelineEventType.VIEW_CLICKED) 0xFF9C27B0 else 0xFF673AB7,
        )
    }

    private fun logAppEvent(
        packageName: String,
        type: TimelineEventType,
        timestamp: Long,
    ) {
        val key = "${type.name}:$packageName"
        if (!shouldLog(key, timestamp)) return
        val appName = AppUtils.getAppName(this, packageName)
        val title =
            when (type) {
                TimelineEventType.APP_OPENED -> "$appName Opened"
                TimelineEventType.APP_CLOSED -> "$appName Closed"
                else -> appName
            }
        logTimeline(
            type = type,
            title = title,
            subtitle = packageName,
            timestamp = timestamp,
            packageName = packageName,
            colorArgb = if (type == TimelineEventType.APP_OPENED) 0xFF2196F3 else 0xFF607D8B,
        )
    }

    private fun shouldLog(
        key: String,
        timestamp: Long,
    ): Boolean {
        val last = recentEventKeys[key]
        if (last != null && timestamp - last < EVENT_DEBOUNCE_MS) return false
        recentEventKeys[key] = timestamp
        if (recentEventKeys.size > MAX_RECENT_EVENTS) {
            recentEventKeys.entries.firstOrNull()?.let { recentEventKeys.remove(it.key) }
        }
        return true
    }

    private fun logTimeline(
        type: TimelineEventType,
        title: String,
        subtitle: String,
        timestamp: Long,
        packageName: String? = null,
        colorArgb: Long = 0xFF6200EE,
    ) {
        scope.launch {
            try {
                timelineRepository.insertEvent(
                    TimelineEvent(
                        type = type,
                        title = title,
                        subtitle = subtitle,
                        timestamp = timestamp,
                        packageName = packageName,
                        colorArgb = colorArgb,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Error logging accessibility event")
            }
        }
    }

    private fun isMessagingPackage(packageName: String): Boolean = packageName in MESSAGING_PACKAGES

    private fun recordDebugEvent(
        eventType: String,
        packageName: String,
        parsedSender: String?,
        parsedMessage: String?,
        nodeCount: Int,
        rawNodesPreview: String,
        confidence: Float?,
    ) {
        accessibilityDebugStore.addEntry(
            AccessibilityDebugEvent(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                eventType = eventType,
                packageName = packageName,
                parsedSender = parsedSender,
                parsedMessage = parsedMessage,
                nodeCount = nodeCount,
                rawNodesPreview = rawNodesPreview,
                confidence = confidence,
            ),
        )
    }

    private fun eventTypeName(eventType: Int): String =
        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "VIEW_TEXT_CHANGED"
            else -> "EVENT_$eventType"
        }

    companion object {
        private const val TAG = "Accessibility"
        private const val EVENT_DEBOUNCE_MS = 1_500L
        private const val MAX_RECENT_EVENTS = 100
        private const val PROCESS_DEBOUNCE_MS = 500L

        val MESSAGING_PACKAGES =
            setOf(
                "org.telegram.messenger",
                "com.whatsapp",
                "com.instagram.android",
            )
    }
}

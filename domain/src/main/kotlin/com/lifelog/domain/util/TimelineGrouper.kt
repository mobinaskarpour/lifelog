package com.lifelog.domain.util

import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.model.TimelineSession
import com.lifelog.utils.DateTimeUtils

object TimelineGrouper {
    private const val SESSION_GAP_MS = 5 * 60 * 1000L
    private const val DEDUP_WINDOW_MS = 2_000L

    fun mergeAppSessions(events: List<TimelineEvent>): List<TimelineEvent> =
        runCatching { mergeAppSessionsInternal(events) }
            .getOrElse { events.sortedByDescending { it.timestamp } }

    private fun mergeAppSessionsInternal(events: List<TimelineEvent>): List<TimelineEvent> {
        val safeEvents = TimelineEventSanitizer.sanitize(events)
        val appEvents =
            deduplicateAppEvents(
                safeEvents.filter {
                    it.type in
                        setOf(
                            TimelineEventType.APP_OPENED,
                            TimelineEventType.APP_CLOSED,
                        )
                },
            ).sortedBy { it.timestamp }

        val otherEvents =
            safeEvents.filter {
                it.type !in
                    setOf(
                        TimelineEventType.APP_OPENED,
                        TimelineEventType.APP_CLOSED,
                    )
            }

        if (appEvents.isEmpty()) return safeEvents.sortedByDescending { it.timestamp }

        val sessions = mutableListOf<TimelineSession>()
        var currentPackage: String? = null
        var currentName: String? = null
        var sessionStart: Long? = null
        var lastTimestamp: Long? = null

        for (event in appEvents) {
            val pkg = event.packageName ?: continue
            val name = event.title.removeSuffix(" Opened").removeSuffix(" Closed").ifBlank { pkg }
            when (event.type) {
                TimelineEventType.APP_OPENED -> {
                    val gap = lastTimestamp?.let { event.timestamp - it } ?: Long.MAX_VALUE
                    if (currentPackage == pkg && sessionStart != null && gap < SESSION_GAP_MS) {
                        lastTimestamp = event.timestamp
                        continue
                    }
                    if (currentPackage != null && sessionStart != null && lastTimestamp != null) {
                        sessions.add(
                            buildSession(
                                currentPackage!!,
                                currentName ?: currentPackage!!,
                                sessionStart!!,
                                lastTimestamp!!,
                            ),
                        )
                    }
                    currentPackage = pkg
                    currentName = name
                    sessionStart = event.timestamp
                    lastTimestamp = event.timestamp
                }
                TimelineEventType.APP_CLOSED -> {
                    if (currentPackage == pkg && sessionStart != null) {
                        lastTimestamp = event.timestamp
                        sessions.add(buildSession(pkg, currentName ?: name, sessionStart!!, event.timestamp))
                        currentPackage = null
                        currentName = null
                        sessionStart = null
                        lastTimestamp = null
                    }
                }
                else -> Unit
            }
        }

        if (currentPackage != null && sessionStart != null && lastTimestamp != null) {
            sessions.add(
                buildSession(
                    currentPackage!!,
                    currentName ?: currentPackage!!,
                    sessionStart!!,
                    lastTimestamp!!,
                ),
            )
        }

        val mergedSessions =
            sessions.mapIndexed { index, session ->
                TimelineEvent(
                    id = syntheticSessionId(session, index),
                    type = TimelineEventType.APP_OPENED,
                    title = session.appName,
                    subtitle =
                        buildString {
                            append("Started: ${DateTimeUtils.formatTime(session.startTime)}")
                            append(" · Ended: ${DateTimeUtils.formatTime(session.endTime)}")
                            append(" · Duration: ${DateTimeUtils.formatDuration(session.durationMs)}")
                        },
                    timestamp = session.startTime,
                    packageName = session.packageName,
                    colorArgb = 0xFF2196F3,
                )
            }

        return (otherEvents + mergedSessions).sortedByDescending { it.timestamp }
    }

    private fun syntheticSessionId(
        session: TimelineSession,
        index: Int,
    ): Long = -(session.startTime + index + session.packageName.hashCode().toLong().and(0x7FFFFFFF))

    private fun deduplicateAppEvents(events: List<TimelineEvent>): List<TimelineEvent> {
        val result = mutableListOf<TimelineEvent>()
        var last: TimelineEvent? = null
        for (event in events.sortedBy { it.timestamp }) {
            val previous = last
            if (previous != null &&
                previous.type == event.type &&
                previous.packageName == event.packageName &&
                event.timestamp - previous.timestamp < DEDUP_WINDOW_MS
            ) {
                continue
            }
            result.add(event)
            last = event
        }
        return result
    }

    private fun buildSession(
        packageName: String,
        appName: String,
        start: Long,
        end: Long,
    ): TimelineSession =
        TimelineSession(
            packageName = packageName,
            appName = appName,
            startTime = start,
            endTime = end.coerceAtLeast(start),
            durationMs = (end - start).coerceAtLeast(0),
        )
}

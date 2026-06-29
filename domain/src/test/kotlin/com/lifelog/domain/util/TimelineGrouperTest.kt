package com.lifelog.domain.util

import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineGrouperTest {
    @Test
    fun mergeAppSessions_combinesOpenAndClose() {
        val events =
            listOf(
                TimelineEvent(
                    id = 1,
                    type = TimelineEventType.APP_OPENED,
                    title = "Telegram Opened",
                    subtitle = "com.telegram.messenger",
                    timestamp = 1_000L,
                    packageName = "com.telegram.messenger",
                ),
                TimelineEvent(
                    id = 2,
                    type = TimelineEventType.APP_CLOSED,
                    title = "Telegram Closed",
                    subtitle = "com.telegram.messenger",
                    timestamp = 3_280_000L,
                    packageName = "com.telegram.messenger",
                ),
                TimelineEvent(
                    id = 3,
                    type = TimelineEventType.NOTIFICATION_RECEIVED,
                    title = "New message",
                    subtitle = "Hello",
                    timestamp = 500L,
                ),
            )

        val merged = TimelineGrouper.mergeAppSessions(events)

        assertEquals(2, merged.size)
        val session = merged.first { it.packageName == "com.telegram.messenger" }
        assertEquals("Telegram", session.title)
        assertTrue(session.subtitle.contains("Started:"))
        assertTrue(session.subtitle.contains("Duration:"))
        assertNotEquals(0L, session.id)
    }

    @Test
    fun mergeAppSessions_deduplicatesRapidDuplicates() {
        val events =
            listOf(
                TimelineEvent(
                    type = TimelineEventType.APP_OPENED,
                    title = "WhatsApp Opened",
                    subtitle = "",
                    timestamp = 1_000L,
                    packageName = "com.whatsapp",
                ),
                TimelineEvent(
                    type = TimelineEventType.APP_OPENED,
                    title = "WhatsApp Opened",
                    subtitle = "",
                    timestamp = 1_500L,
                    packageName = "com.whatsapp",
                ),
                TimelineEvent(
                    type = TimelineEventType.APP_CLOSED,
                    title = "WhatsApp Closed",
                    subtitle = "",
                    timestamp = 5_000L,
                    packageName = "com.whatsapp",
                ),
            )

        val merged = TimelineGrouper.mergeAppSessions(events)
        val sessions = merged.filter { it.packageName == "com.whatsapp" }
        assertEquals(1, sessions.size)
    }

    @Test
    fun mergeAppSessions_assignsUniqueIdsToMergedSessions() {
        val events =
            listOf(
                TimelineEvent(
                    id = 1,
                    type = TimelineEventType.APP_OPENED,
                    title = "Telegram Opened",
                    subtitle = "",
                    timestamp = 1_000L,
                    packageName = "com.telegram.messenger",
                ),
                TimelineEvent(
                    id = 2,
                    type = TimelineEventType.APP_CLOSED,
                    title = "Telegram Closed",
                    subtitle = "",
                    timestamp = 2_000L,
                    packageName = "com.telegram.messenger",
                ),
                TimelineEvent(
                    id = 3,
                    type = TimelineEventType.APP_OPENED,
                    title = "WhatsApp Opened",
                    subtitle = "",
                    timestamp = 3_000L,
                    packageName = "com.whatsapp",
                ),
                TimelineEvent(
                    id = 4,
                    type = TimelineEventType.APP_CLOSED,
                    title = "WhatsApp Closed",
                    subtitle = "",
                    timestamp = 4_000L,
                    packageName = "com.whatsapp",
                ),
            )

        val merged = TimelineGrouper.mergeAppSessions(events)
        val sessionIds = merged.filter { it.id < 0 }.map { it.id }.toSet()
        assertEquals(2, sessionIds.size)
    }

    @Test
    fun mergeAppSessions_handlesEmptyList() {
        assertTrue(TimelineGrouper.mergeAppSessions(emptyList()).isEmpty())
    }
}

package com.lifelog.domain.util

import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEventSanitizerTest {
    @Test
    fun sanitize_skipsNegativeTimestamp() {
        val event =
            TimelineEvent(
                type = TimelineEventType.APP_OPENED,
                title = "Test",
                subtitle = "",
                timestamp = -1L,
            )
        assertNull(TimelineEventSanitizer.sanitizeOne(event))
    }

    @Test
    fun sanitize_fillsBlankTitle() {
        val event =
            TimelineEvent(
                type = TimelineEventType.NOTIFICATION_RECEIVED,
                title = "",
                subtitle = "Body",
                timestamp = 1_000L,
            )
        val sanitized = TimelineEventSanitizer.sanitizeOne(event)
        assertNotNull(sanitized)
        assertEquals("Notification", sanitized!!.title)
    }

    @Test
    fun stableKey_isUniqueForDuplicateIds() {
        val event =
            TimelineEvent(
                id = 0,
                type = TimelineEventType.APP_OPENED,
                title = "App",
                subtitle = "",
                timestamp = 1_000L,
                packageName = "com.example",
            )
        val key0 = TimelineEventSanitizer.stableKey(event, 0)
        val key1 = TimelineEventSanitizer.stableKey(event, 1)
        assertNotNull(key0)
        assertTrue(key0 != key1)
    }
}

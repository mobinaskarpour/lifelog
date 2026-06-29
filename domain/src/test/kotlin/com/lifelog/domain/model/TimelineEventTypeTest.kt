package com.lifelog.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineEventTypeTest {
    @Test
    fun fromString_returnsKnownType() {
        assertEquals(TimelineEventType.APP_OPENED, TimelineEventType.fromString("APP_OPENED"))
    }

    @Test
    fun fromString_returnsNullForUnknownType() {
        assertNull(TimelineEventType.fromString("INVALID_LEGACY_TYPE"))
        assertNull(TimelineEventType.fromString(null))
        assertNull(TimelineEventType.fromString(""))
    }
}

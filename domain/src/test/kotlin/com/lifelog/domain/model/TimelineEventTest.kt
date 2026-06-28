package com.lifelog.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineEventTest {

    @Test
    fun timelineEventType_hasAllRequiredTypes() {
        val types = TimelineEventType.entries
        assertEquals(true, types.contains(TimelineEventType.PHONE_UNLOCKED))
        assertEquals(true, types.contains(TimelineEventType.APP_OPENED))
        assertEquals(true, types.contains(TimelineEventType.NOTIFICATION_RECEIVED))
    }

    @Test
    fun dashboardStats_defaultValues() {
        val stats = DashboardStats()
        assertEquals(0L, stats.screenTimeMs)
        assertEquals(0, stats.appLaunchCount)
        assertEquals(true, stats.topApps.isEmpty())
    }
}

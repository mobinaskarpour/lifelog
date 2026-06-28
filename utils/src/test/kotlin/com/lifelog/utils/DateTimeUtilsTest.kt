package com.lifelog.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeUtilsTest {
    @Test
    fun formatDuration_hoursAndMinutes() {
        val result = DateTimeUtils.formatDuration(3_660_000)
        assertEquals("1h 01m", result)
    }

    @Test
    fun formatDuration_minutesAndSeconds() {
        val result = DateTimeUtils.formatDuration(125_000)
        assertEquals("2m 05s", result)
    }

    @Test
    fun formatDuration_secondsOnly() {
        val result = DateTimeUtils.formatDuration(45_000)
        assertEquals("45s", result)
    }

    @Test
    fun startOfDay_returnsMidnight() {
        val cal =
            java.util.Calendar.getInstance().apply {
                set(2025, 5, 15, 14, 30, 45)
                set(java.util.Calendar.MILLISECOND, 500)
            }
        val start = DateTimeUtils.startOfDay(cal.timeInMillis)
        val startCal = java.util.Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(0, startCal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(java.util.Calendar.MINUTE))
        assertEquals(0, startCal.get(java.util.Calendar.SECOND))
    }
}

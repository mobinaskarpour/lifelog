package com.lifelog.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatTime(timestamp: Long): String =
        runCatching {
            timeFormat.format(Date(timestamp.coerceAtLeast(0L)))
        }.getOrDefault("--:--")

    fun formatDateTime(timestamp: Long): String =
        runCatching {
            dateTimeFormat.format(Date(timestamp.coerceAtLeast(0L)))
        }.getOrDefault("Unknown time")

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun startOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun formatDuration(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val hours = TimeUnit.MILLISECONDS.toHours(safeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safeMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safeMillis) % 60
        return when {
            hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
            minutes > 0 -> String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds)
            else -> String.format(Locale.getDefault(), "%ds", seconds)
        }
    }

    fun daysAgoDate(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return dateFormat.format(cal.time)
    }

    fun yesterdayDate(): String = daysAgoDate(1)

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> formatTime(timestamp)
            else -> formatDateTime(timestamp)
        }
    }

    fun formatChatDateSeparator(timestamp: Long): String {
        val todayStart = startOfDay()
        val yesterdayStart = startOfDay(todayStart - 1)
        val messageDay = startOfDay(timestamp)
        return when {
            messageDay >= todayStart -> "Today"
            messageDay >= yesterdayStart -> "Yesterday"
            else -> formatDate(timestamp)
        }
    }
}

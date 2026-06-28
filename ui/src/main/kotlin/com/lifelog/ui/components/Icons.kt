package com.lifelog.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.material.icons.outlined.PhoneMissed
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lifelog.domain.model.CallType
import com.lifelog.domain.model.TimelineEventType

fun timelineIcon(type: TimelineEventType): ImageVector = when (type) {
    TimelineEventType.PHONE_UNLOCKED -> Icons.Outlined.LockOpen
    TimelineEventType.APP_OPENED, TimelineEventType.APP_CLOSED -> Icons.Filled.Apps
    TimelineEventType.INCOMING_CALL -> Icons.Outlined.PhoneCallback
    TimelineEventType.OUTGOING_CALL -> Icons.Outlined.Phone
    TimelineEventType.MISSED_CALL -> Icons.Outlined.PhoneMissed
    TimelineEventType.CALL_ENDED -> Icons.Outlined.Call
    TimelineEventType.NOTIFICATION_RECEIVED -> Icons.Outlined.Notifications
    TimelineEventType.SCREEN_ON, TimelineEventType.SCREEN_OFF -> Icons.Outlined.ScreenLockPortrait
    TimelineEventType.BATTERY_CHANGED -> Icons.Outlined.PowerSettingsNew
    TimelineEventType.WIFI_CONNECTED, TimelineEventType.WIFI_DISCONNECTED -> Icons.Outlined.Wifi
    else -> Icons.Filled.History
}

fun timelineColor(type: TimelineEventType): Color = when (type) {
    TimelineEventType.PHONE_UNLOCKED -> Color(0xFF4CAF50)
    TimelineEventType.APP_OPENED -> Color(0xFF2196F3)
    TimelineEventType.APP_CLOSED -> Color(0xFF607D8B)
    TimelineEventType.INCOMING_CALL -> Color(0xFF4CAF50)
    TimelineEventType.OUTGOING_CALL -> Color(0xFF2196F3)
    TimelineEventType.MISSED_CALL -> Color(0xFFF44336)
    TimelineEventType.CALL_ENDED -> Color(0xFF9E9E9E)
    TimelineEventType.NOTIFICATION_RECEIVED -> Color(0xFFFF9800)
    TimelineEventType.SCREEN_ON -> Color(0xFF8BC34A)
    TimelineEventType.SCREEN_OFF -> Color(0xFF795548)
    TimelineEventType.BATTERY_CHANGED -> Color(0xFFFFC107)
    TimelineEventType.WIFI_CONNECTED -> Color(0xFF03A9F4)
    TimelineEventType.WIFI_DISCONNECTED -> Color(0xFF9E9E9E)
    else -> Color(0xFF6200EE)
}

fun callIcon(type: CallType): ImageVector = when (type) {
    CallType.INCOMING -> Icons.Outlined.PhoneCallback
    CallType.OUTGOING -> Icons.Outlined.Phone
    CallType.MISSED -> Icons.Outlined.PhoneMissed
}

fun callColor(type: CallType): Color = when (type) {
    CallType.INCOMING -> Color(0xFF4CAF50)
    CallType.OUTGOING -> Color(0xFF2196F3)
    CallType.MISSED -> Color(0xFFF44336)
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems =
    listOf(
        BottomNavItem("dashboard", "Dashboard", Icons.Filled.Dashboard),
        BottomNavItem("timeline", "Timeline", Icons.Filled.History),
        BottomNavItem("apps", "Apps", Icons.Filled.Apps),
        BottomNavItem("notifications", "Alerts", Icons.Filled.Notifications),
        BottomNavItem("settings", "Settings", Icons.Filled.Settings),
    )

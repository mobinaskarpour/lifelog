package com.lifelog.feature.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
)

object PermissionHelper {
    fun getPermissionItems(context: Context): List<PermissionItem> =
        listOf(
            PermissionItem(
                id = "usage",
                title = "Usage Access",
                description = "Required to track which apps you open, screen time, and app launch counts.",
                isGranted = hasUsageAccess(context),
            ),
            PermissionItem(
                id = "notification",
                title = "Notification Access",
                description = "Allows LifeLog to read incoming notifications and build your notification history.",
                isGranted = isNotificationListenerEnabled(context),
            ),
            PermissionItem(
                id = "accessibility",
                title = "Accessibility Service",
                description = "Enhances screen event detection including device unlock tracking.",
                isGranted = isAccessibilityEnabled(context),
            ),
            PermissionItem(
                id = "location",
                title = "Location",
                description = "Optional. Records periodic GPS location when enabled in settings.",
                isGranted = hasLocationPermission(context),
            ),
            PermissionItem(
                id = "phone",
                title = "Phone",
                description = "Required to log incoming, outgoing, and missed calls.",
                isGranted = hasPhonePermission(context),
            ),
            PermissionItem(
                id = "sms",
                title = "SMS",
                description = "Required to import inbox, sent, draft, and outbox SMS from this device.",
                isGranted = hasSmsPermission(context),
            ),
            PermissionItem(
                id = "battery",
                title = "Battery Optimization Exemption",
                description = "Prevents the system from stopping background tracking to ensure continuous logging.",
                isGranted = isBatteryOptimizationDisabled(context),
            ),
        )

    fun openPermissionSettings(
        context: Context,
        permissionId: String,
    ) {
        val intent =
            when (permissionId) {
                "usage" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                "notification" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                "location" ->
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                "phone" ->
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                "sms" ->
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                "battery" ->
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                else -> return
            }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode =
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat =
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
        return flat.contains(context.packageName)
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val flat =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
        return flat.contains(context.packageName)
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun hasPhonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun isBatteryOptimizationDisabled(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun allRequiredGranted(context: Context): Boolean {
        val items = getPermissionItems(context)
        return items.filter { it.id != "location" }.all { it.isGranted }
    }
}

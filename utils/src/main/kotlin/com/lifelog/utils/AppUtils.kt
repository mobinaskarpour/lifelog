package com.lifelog.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache

object AppUtils {
    private const val LABEL_CACHE_SIZE = 256
    private const val ICON_CACHE_SIZE = 128

    private val labelCache = LruCache<String, String>(LABEL_CACHE_SIZE)
    private val iconCache = LruCache<String, Drawable?>(ICON_CACHE_SIZE)

    private val ignoredPackages =
        setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.android.launcher",
            "com.sec.android.app.launcher",
            "com.google.android.apps.nexuslauncher",
        )

    fun shouldTrackPackage(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (ignoredPackages.any { packageName.startsWith(it) }) return false
        return !packageName.startsWith("com.android.") || packageName.startsWith("com.android.chrome")
    }

    fun getAppName(
        context: Context,
        packageName: String,
    ): String {
        labelCache.get(packageName)?.let { return it }
        val label =
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString().trim()
            } catch (_: PackageManager.NameNotFoundException) {
                packageName
            }
        labelCache.put(packageName, label)
        return label
    }

    fun getAppIcon(
        context: Context,
        packageName: String,
    ): Drawable? {
        iconCache.get(packageName)?.let { return it }
        return try {
            val icon = context.packageManager.getApplicationIcon(packageName)
            iconCache.put(packageName, icon)
            icon
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun clearCache() {
        labelCache.evictAll()
        iconCache.evictAll()
    }
}

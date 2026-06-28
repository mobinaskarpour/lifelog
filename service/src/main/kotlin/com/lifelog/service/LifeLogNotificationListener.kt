package com.lifelog.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.lifelog.domain.model.NotificationLog
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.repository.NotificationRepository
import com.lifelog.domain.repository.TimelineRepository
import com.lifelog.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LifeLogNotificationListener : NotificationListenerService() {
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var timelineRepository: TimelineRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        val packageName = sbn.packageName
        if (packageName == this.packageName) return

        val title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: ""
        val appName = AppUtils.getAppName(this, packageName)
        val timestamp = sbn.postTime

        scope.launch {
            try {
                notificationRepository.insertNotification(
                    NotificationLog(
                        appName = appName,
                        packageName = packageName,
                        title = title,
                        timestamp = timestamp,
                    ),
                )
                timelineRepository.insertEvent(
                    TimelineEvent(
                        type = TimelineEventType.NOTIFICATION_RECEIVED,
                        title = "Notification Received",
                        subtitle = "$appName: $title",
                        timestamp = timestamp,
                        packageName = packageName,
                        colorArgb = 0xFFFF9800,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Error logging notification")
            }
        }
    }
}

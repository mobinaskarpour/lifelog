package com.lifelog.service

import android.app.Notification
import android.os.Bundle
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
    private val lastTimelineKeys = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        val packageName = sbn.packageName
        if (packageName == this.packageName) return

        val extras = sbn.notification.extras
        val appName = AppUtils.getAppName(this, packageName)
        val title = extractTitle(extras)
        val text = extractText(extras)
        val subtext = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val conversationName = extractConversationName(extras)
        val timestamp = sbn.postTime
        val notificationId = sbn.id

        scope.launch {
            try {
                notificationRepository.upsertNotification(
                    NotificationLog(
                        appName = appName,
                        packageName = packageName,
                        title = title,
                        text = text,
                        subtext = subtext,
                        bigText = bigText,
                        notificationId = notificationId,
                        conversationName = conversationName,
                        timestamp = timestamp,
                    ),
                )

                val timelineKey = "$packageName:$notificationId"
                if (lastTimelineKeys.add(timelineKey)) {
                    val body = bigText?.takeIf { it.isNotBlank() } ?: text.ifBlank { subtext }
                    val subtitle =
                        buildString {
                            conversationName?.takeIf { it.isNotBlank() }?.let {
                                append(it)
                                if (body.isNotBlank()) append(": ")
                            }
                            append(body)
                        }.ifBlank { title }

                    timelineRepository.insertEvent(
                        TimelineEvent(
                            type = TimelineEventType.NOTIFICATION_RECEIVED,
                            title = title.ifBlank { appName },
                            subtitle = subtitle,
                            timestamp = timestamp,
                            packageName = packageName,
                            colorArgb = 0xFFFF9800,
                        ),
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error logging notification")
            }
        }
    }

    private fun extractTitle(extras: Bundle): String =
        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: ""

    private fun extractText(extras: Bundle): String =
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
            ?: ""

    private fun extractConversationName(extras: Bundle): String? =
        extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras.getCharSequence("android.hiddenConversationTitle")?.toString()
}

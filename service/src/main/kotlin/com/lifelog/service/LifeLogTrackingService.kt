package com.lifelog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lifelog.domain.repository.SettingsRepository
import com.lifelog.service.receiver.BatteryReceiver
import com.lifelog.service.receiver.MonitoringStopReceiver
import com.lifelog.service.receiver.NetworkReceiver
import com.lifelog.service.sms.SmsSyncManager
import com.lifelog.utils.DateTimeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LifeLogTrackingService : Service() {
    @Inject lateinit var usageStatsTracker: UsageStatsTracker

    @Inject lateinit var batteryRepository: com.lifelog.domain.repository.BatteryRepository

    @Inject lateinit var timelineRepository: com.lifelog.domain.repository.TimelineRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var locationTracker: LocationTracker

    @Inject lateinit var smsSyncManager: SmsSyncManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var trackingJob: Job? = null
    private var stoppedByUser = false

    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var networkReceiver: NetworkReceiver

    override fun onCreate() {
        super.onCreate()
        batteryReceiver = BatteryReceiver(batteryRepository, timelineRepository)
        networkReceiver = NetworkReceiver(timelineRepository)
        createNotificationChannel()
        startForeground(TrackingActions.NOTIFICATION_ID, buildPlaceholderNotification())
        registerReceiver(batteryReceiver, BatteryReceiver.intentFilter())
        registerReceiver(networkReceiver, NetworkReceiver.intentFilter())
        locationTracker.startTracking()
        smsSyncManager.start()
        serviceScope.launch {
            val settings = settingsRepository.getSettings().first()
            if (settings.monitoringStartedAt == 0L) {
                settingsRepository.setMonitoringStartedAt(System.currentTimeMillis())
            }
            settingsRepository.setMonitoringEnabled(true)
            refreshNotification()
        }
        startTracking()
        Timber.d("LifeLog tracking service started")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == TrackingActions.ACTION_STOP_MONITORING) {
            serviceScope.launch { stopMonitoring() }
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        trackingJob?.cancel()
        locationTracker.stopTracking()
        smsSyncManager.stop()
        try {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(networkReceiver)
        } catch (_: Exception) {
        }
        if (stoppedByUser) {
            getSystemService(NotificationManager::class.java)
                ?.cancel(TrackingActions.NOTIFICATION_ID)
        }
        super.onDestroy()
        Timber.d("LifeLog tracking service stopped")
    }

    private fun startTracking() {
        trackingJob =
            serviceScope.launch {
                while (isActive) {
                    usageStatsTracker.trackUsage()
                    refreshNotification()
                    delay(TRACKING_INTERVAL_MS)
                }
            }
    }

    private suspend fun stopMonitoring() {
        stoppedByUser = true
        settingsRepository.setMonitoringEnabled(false)
        trackingJob?.cancel()
        locationTracker.stopTracking()
        smsSyncManager.stop()
        try {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(networkReceiver)
        } catch (_: Exception) {
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java)?.cancel(TrackingActions.NOTIFICATION_ID)
        stopSelf()
    }

    private suspend fun refreshNotification() {
        val settings = settingsRepository.getSettings().first()
        val startedAt = settings.monitoringStartedAt
        val runningMs =
            if (startedAt > 0) {
                (System.currentTimeMillis() - startedAt).coerceAtLeast(0)
            } else {
                0L
            }
        val today = DateTimeUtils.startOfDay()
        val eventsToday = timelineRepository.getEventsForDate(today).first().size
        val notification = buildNotification(settings.lastOpenedRoute, startedAt, runningMs, eventsToday)
        getSystemService(NotificationManager::class.java)?.notify(TrackingActions.NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                TrackingActions.CHANNEL_ID,
                "LifeLog",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "LifeLog background monitoring status"
                setShowBadge(false)
            }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildPlaceholderNotification(): Notification =
        NotificationCompat.Builder(this, TrackingActions.CHANNEL_ID)
            .setContentTitle("Monitoring Active")
            .setContentText("Monitoring device activity in the background")
            .setSmallIcon(R.drawable.ic_stat_lifelog)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun buildNotification(
        lastRoute: String,
        startedAt: Long,
        runningMs: Long,
        eventsToday: Int,
    ): Notification {
        val startedText =
            if (startedAt > 0) {
                DateTimeUtils.formatTime(startedAt)
            } else {
                "—"
            }
        val liveDetails =
            buildString {
                append("Started at $startedText\n")
                append("Events today: $eventsToday\n")
                append("Running time: ${DateTimeUtils.formatDuration(runningMs)}")
            }

        val openIntent =
            Intent().apply {
                setClassName(packageName, TrackingActions.MAIN_ACTIVITY_CLASS)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TrackingActions.EXTRA_ROUTE, lastRoute)
            }
        val openPendingIntent =
            PendingIntent.getActivity(
                this,
                REQUEST_OPEN_APP,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val stopIntent =
            Intent(this, MonitoringStopReceiver::class.java).apply {
                action = TrackingActions.ACTION_STOP_MONITORING
            }
        val stopPendingIntent =
            PendingIntent.getBroadcast(
                this,
                REQUEST_STOP_MONITORING,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Builder(this, TrackingActions.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lifelog)
            .setContentTitle("Monitoring Active")
            .setContentText("Monitoring device activity in the background")
            .setSubText("LifeLog")
            .setStyle(NotificationCompat.BigTextStyle().bigText(liveDetails))
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Monitoring",
                stopPendingIntent,
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        private const val TRACKING_INTERVAL_MS = 30_000L
        private const val REQUEST_OPEN_APP = 100
        private const val REQUEST_STOP_MONITORING = 101
    }
}

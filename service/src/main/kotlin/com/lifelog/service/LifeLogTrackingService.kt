package com.lifelog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lifelog.service.receiver.BatteryReceiver
import com.lifelog.service.receiver.NetworkReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LifeLogTrackingService : Service() {
    @Inject lateinit var usageStatsTracker: UsageStatsTracker
    @Inject lateinit var batteryRepository: com.lifelog.domain.repository.BatteryRepository
    @Inject lateinit var timelineRepository: com.lifelog.domain.repository.TimelineRepository
    @Inject lateinit var locationTracker: LocationTracker

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var trackingJob: Job? = null

    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var networkReceiver: NetworkReceiver

    override fun onCreate() {
        super.onCreate()
        batteryReceiver = BatteryReceiver(batteryRepository, timelineRepository)
        networkReceiver = NetworkReceiver(timelineRepository)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerReceiver(batteryReceiver, BatteryReceiver.intentFilter())
        registerReceiver(networkReceiver, NetworkReceiver.intentFilter())
        locationTracker.startTracking()
        startTracking()
        Timber.d("LifeLog tracking service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        trackingJob?.cancel()
        locationTracker.stopTracking()
        try {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(networkReceiver)
        } catch (_: Exception) { }
        super.onDestroy()
        Timber.d("LifeLog tracking service stopped")
    }

    private fun startTracking() {
        trackingJob = serviceScope.launch {
            while (isActive) {
                usageStatsTracker.trackUsage()
                delay(TRACKING_INTERVAL_MS)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LifeLog Tracking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Background activity tracking"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LifeLog")
            .setContentText("Tracking your activity")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "lifelog_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val TRACKING_INTERVAL_MS = 30_000L
    }
}

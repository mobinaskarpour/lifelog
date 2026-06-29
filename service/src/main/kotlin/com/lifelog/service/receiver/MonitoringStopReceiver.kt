package com.lifelog.service.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifelog.service.LifeLogTrackingService
import com.lifelog.service.TrackingActions
import com.lifelog.service.di.MonitoringEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MonitoringStopReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != TrackingActions.ACTION_STOP_MONITORING) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepository =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        MonitoringEntryPoint::class.java,
                    ).settingsRepository()
                settingsRepository.setMonitoringEnabled(false)

                context.stopService(Intent(context, LifeLogTrackingService::class.java))
                context.getSystemService(NotificationManager::class.java)
                    ?.cancel(TrackingActions.NOTIFICATION_ID)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

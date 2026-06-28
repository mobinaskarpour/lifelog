package com.lifelog.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.lifelog.domain.model.BatteryLog
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.repository.BatteryRepository
import com.lifelog.domain.repository.TimelineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class BatteryReceiver(
    private val batteryRepository: BatteryRepository,
    private val timelineRepository: TimelineRepository,
) : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
            Intent.ACTION_POWER_CONNECTED -> logPowerEvent("Power Connected", true)
            Intent.ACTION_POWER_DISCONNECTED -> logPowerEvent("Power Disconnected", false)
        }
    }

    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f

        scope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                batteryRepository.insertBatteryLog(
                    BatteryLog(
                        level = batteryPct,
                        isCharging = isCharging,
                        temperature = temperature,
                        isPowerConnected = isCharging,
                        timestamp = timestamp,
                    ),
                )
                timelineRepository.insertEvent(
                    TimelineEvent(
                        type = TimelineEventType.BATTERY_CHANGED,
                        title = "Battery $batteryPct%",
                        subtitle = if (isCharging) "Charging" else "Not charging",
                        timestamp = timestamp,
                        colorArgb = 0xFFFFC107,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Error logging battery")
            }
        }
    }

    private fun logPowerEvent(title: String, connected: Boolean) {
        scope.launch {
            timelineRepository.insertEvent(
                TimelineEvent(
                    type = TimelineEventType.BATTERY_CHANGED,
                    title = title,
                    subtitle = if (connected) "Charger plugged in" else "Charger unplugged",
                    timestamp = System.currentTimeMillis(),
                    colorArgb = 0xFFFFC107,
                ),
            )
        }
    }

    companion object {
        fun intentFilter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
    }
}

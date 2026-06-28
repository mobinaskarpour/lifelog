package com.lifelog.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.repository.TimelineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class NetworkReceiver(
    private val timelineRepository: TimelineRepository,
) : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> handleConnectivity(context)
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val isOn = intent.getBooleanExtra("state", false)
                logNetworkEvent(
                    if (isOn) TimelineEventType.AIRPLANE_MODE_ON else TimelineEventType.AIRPLANE_MODE_OFF,
                    if (isOn) "Airplane Mode On" else "Airplane Mode Off",
                )
            }
            android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, -1)
                when (state) {
                    android.bluetooth.BluetoothAdapter.STATE_ON ->
                        logNetworkEvent(TimelineEventType.BLUETOOTH_ON, "Bluetooth On")
                    android.bluetooth.BluetoothAdapter.STATE_OFF ->
                        logNetworkEvent(TimelineEventType.BLUETOOTH_OFF, "Bluetooth Off")
                }
            }
        }
    }

    private fun handleConnectivity(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }

        when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ->
                logNetworkEvent(TimelineEventType.WIFI_CONNECTED, "WiFi Connected")
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ->
                logNetworkEvent(TimelineEventType.MOBILE_DATA_ON, "Mobile Data On")
            else -> {
                logNetworkEvent(TimelineEventType.WIFI_DISCONNECTED, "WiFi Disconnected")
                logNetworkEvent(TimelineEventType.MOBILE_DATA_OFF, "Mobile Data Off")
            }
        }
    }

    private fun logNetworkEvent(
        type: TimelineEventType,
        title: String,
    ) {
        scope.launch {
            try {
                timelineRepository.insertEvent(
                    TimelineEvent(
                        type = type,
                        title = title,
                        subtitle = "",
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Error logging network event")
            }
        }
    }

    companion object {
        fun intentFilter(): IntentFilter =
            IntentFilter().apply {
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
            }
    }
}

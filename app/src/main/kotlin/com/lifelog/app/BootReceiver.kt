package com.lifelog.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifelog.domain.repository.SettingsRepository
import com.lifelog.service.worker.ServiceStarter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ServiceStarter.startTrackingIfEnabled(context, settingsRepository)
                Timber.d("LifeLog tracking restarted after boot")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

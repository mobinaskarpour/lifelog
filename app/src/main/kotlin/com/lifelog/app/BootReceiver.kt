package com.lifelog.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifelog.service.worker.ServiceStarter
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ServiceStarter.startTracking(context)
            Timber.d("LifeLog tracking restarted after boot")
        }
    }
}

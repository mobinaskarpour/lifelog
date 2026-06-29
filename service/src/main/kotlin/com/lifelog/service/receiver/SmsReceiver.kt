package com.lifelog.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.lifelog.service.di.MonitoringEntryPoint
import com.lifelog.service.sms.SmsSyncManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val syncManager = context.smsSyncManager()
                syncManager.requestFollowUpSync()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun Context.smsSyncManager(): SmsSyncManager {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, MonitoringEntryPoint::class.java)
        return entryPoint.smsSyncManager()
    }
}

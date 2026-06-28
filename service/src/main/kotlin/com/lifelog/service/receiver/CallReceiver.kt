package com.lifelog.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.CallLog as SystemCallLog
import com.lifelog.domain.model.CallLog
import com.lifelog.domain.model.CallType
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.repository.CallRepository
import com.lifelog.domain.repository.TimelineRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CallReceiver : BroadcastReceiver() {

    @Inject lateinit var callRepository: CallRepository
    @Inject lateinit var timelineRepository: TimelineRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: "Unknown"
                logCall(CallType.OUTGOING, number, 0)
            }
            "android.intent.action.PHONE_STATE" -> {
                val state = intent.getStringExtra("state")
                val number = intent.getStringExtra(SystemCallLog.Calls.INCOMING_NUMBER) ?: "Unknown"
                if (state == SystemCallLog.Calls.INCOMING_TYPE.toString() ||
                    state == android.telephony.TelephonyManager.EXTRA_STATE_RINGING
                ) {
                    logCall(CallType.INCOMING, number, 0)
                }
            }
        }
    }

    private fun logCall(type: CallType, number: String, duration: Long) {
        scope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val timelineType = when (type) {
                    CallType.INCOMING -> TimelineEventType.INCOMING_CALL
                    CallType.OUTGOING -> TimelineEventType.OUTGOING_CALL
                    CallType.MISSED -> TimelineEventType.MISSED_CALL
                }
                val title = when (type) {
                    CallType.INCOMING -> "Incoming Call"
                    CallType.OUTGOING -> "Outgoing Call"
                    CallType.MISSED -> "Missed Call"
                }
                callRepository.insertCall(
                    CallLog(
                        phoneNumber = number,
                        contactName = null,
                        type = type,
                        duration = duration,
                        timestamp = timestamp,
                    ),
                )
                timelineRepository.insertEvent(
                    TimelineEvent(
                        type = timelineType,
                        title = title,
                        subtitle = number,
                        timestamp = timestamp,
                        colorArgb = when (type) {
                            CallType.INCOMING -> 0xFF4CAF50
                            CallType.OUTGOING -> 0xFF2196F3
                            CallType.MISSED -> 0xFFF44336
                        },
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Error logging call")
            }
        }
    }

    companion object {
        fun intentFilter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_NEW_OUTGOING_CALL)
            addAction("android.intent.action.PHONE_STATE")
        }
    }
}

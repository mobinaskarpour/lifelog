package com.lifelog.service.sms

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import com.lifelog.domain.model.SmsAccessStatus
import com.lifelog.domain.repository.SmsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSyncManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val smsProviderReader: SmsProviderReader,
        private val smsRepository: SmsRepository,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val _accessStatus = MutableStateFlow(SmsAccessStatus.PERMISSION_DENIED)
        val accessStatus: StateFlow<SmsAccessStatus> = _accessStatus.asStateFlow()

        private val _lastSyncMessage = MutableStateFlow<String?>(null)
        val lastSyncMessage: StateFlow<String?> = _lastSyncMessage.asStateFlow()

        private var contentObserver: ContentObserver? = null
        private var isObserving = false

        fun start() {
            scope.launch { syncFromProvider() }
            startObserving()
        }

        fun stop() {
            contentObserver?.let { context.contentResolver.unregisterContentObserver(it) }
            contentObserver = null
            isObserving = false
        }

        suspend fun syncFromProvider() {
            _accessStatus.value = smsProviderReader.getAccessStatus()
            when (val result = smsProviderReader.readAllMessages()) {
                is SmsReadResult.Success -> {
                    _accessStatus.value = SmsAccessStatus.GRANTED
                    smsRepository.upsertMessages(result.messages)
                    smsRepository.updateProviderStats(result.providerStats)
                    _lastSyncMessage.value = null
                    Timber.d(
                        "SMS sync complete: ${result.messages.size} messages, " +
                            "sent=${result.providerStats.providerSentCount}",
                    )
                }
                is SmsReadResult.Failure -> {
                    _accessStatus.value = result.status
                    _lastSyncMessage.value = result.message
                    Timber.w("SMS sync failed: ${result.message}")
                }
            }
        }

        private fun startObserving() {
            if (isObserving) return
            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        onChange(selfChange, null)
                    }

                    override fun onChange(
                        selfChange: Boolean,
                        uri: Uri?,
                    ) {
                        scope.launch { syncFromProvider() }
                    }
                }
            context.contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer,
            )
            contentObserver = observer
            isObserving = true
        }
    }

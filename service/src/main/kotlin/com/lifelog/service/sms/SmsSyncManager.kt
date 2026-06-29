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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        private val syncMutex = Mutex()
        private val _accessStatus = MutableStateFlow(SmsAccessStatus.PERMISSION_DENIED)
        val accessStatus: StateFlow<SmsAccessStatus> = _accessStatus.asStateFlow()

        private val _lastSyncMessage = MutableStateFlow<String?>(null)
        val lastSyncMessage: StateFlow<String?> = _lastSyncMessage.asStateFlow()

        private var contentObserver: ContentObserver? = null
        private var isObserving = false
        private var followUpSyncJob: Job? = null
        private var outgoingPollJob: Job? = null

        fun start() {
            scope.launch { syncFromProvider() }
            startObserving()
            startOutgoingPolling()
        }

        fun stop() {
            followUpSyncJob?.cancel()
            followUpSyncJob = null
            outgoingPollJob?.cancel()
            outgoingPollJob = null
            contentObserver?.let { context.contentResolver.unregisterContentObserver(it) }
            contentObserver = null
            isObserving = false
        }

        suspend fun syncFromProvider() {
            syncMutex.withLock {
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
        }

        fun requestFollowUpSync() {
            scheduleFollowUpSync()
        }

        private fun scheduleFollowUpSync() {
            followUpSyncJob?.cancel()
            followUpSyncJob =
                scope.launch {
                    OBSERVER_FOLLOW_UP_DELAYS_MS.forEachIndexed { index, delayMs ->
                        if (delayMs > 0L) {
                            delay(delayMs)
                        }
                        if (index == 0) {
                            syncFromProvider()
                        } else {
                            syncRecentOutgoing()
                        }
                    }
                }
        }

        private suspend fun syncRecentOutgoing() {
            if (!smsProviderReader.hasReadPermission()) return
            syncMutex.withLock {
                val since = System.currentTimeMillis() - RECENT_OUTGOING_WINDOW_MS
                when (val result = smsProviderReader.readRecentOutgoing(since)) {
                    is SmsReadResult.Success -> {
                        if (result.messages.isNotEmpty()) {
                            smsRepository.upsertMessages(result.messages)
                            Timber.d("SMS outgoing poll: upserted ${result.messages.size} recent messages")
                        }
                    }
                    is SmsReadResult.Failure -> {
                        Timber.w("SMS outgoing poll failed: ${result.message}")
                    }
                }
            }
        }

        private fun startOutgoingPolling() {
            outgoingPollJob?.cancel()
            outgoingPollJob =
                scope.launch {
                    while (isActive) {
                        delay(OUTGOING_POLL_INTERVAL_MS)
                        syncRecentOutgoing()
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
                        scheduleFollowUpSync()
                    }
                }
            OBSERVED_URIS.forEach { uri ->
                context.contentResolver.registerContentObserver(
                    uri,
                    true,
                    observer,
                )
            }
            contentObserver = observer
            isObserving = true
        }

        companion object {
            private val OBSERVER_FOLLOW_UP_DELAYS_MS = longArrayOf(0L, 400L, 1_000L, 2_000L)
            private const val OUTGOING_POLL_INTERVAL_MS = 15_000L
            private const val RECENT_OUTGOING_WINDOW_MS = 5 * 60 * 1_000L

            private val OBSERVED_URIS =
                listOf(
                    Telephony.Sms.CONTENT_URI,
                    Telephony.Sms.Sent.CONTENT_URI,
                    Telephony.Sms.Outbox.CONTENT_URI,
                )
        }
    }

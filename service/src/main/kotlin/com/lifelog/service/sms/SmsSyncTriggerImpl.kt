package com.lifelog.service.sms

import com.lifelog.domain.sync.SmsSyncTrigger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSyncTriggerImpl
    @Inject
    constructor(
        private val smsSyncManager: SmsSyncManager,
    ) : SmsSyncTrigger {
        override suspend fun syncFromProvider() {
            smsSyncManager.syncFromProvider()
        }

        override fun requestFollowUpSync() {
            smsSyncManager.requestFollowUpSync()
        }
    }

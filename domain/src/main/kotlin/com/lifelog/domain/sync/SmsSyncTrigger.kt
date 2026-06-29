package com.lifelog.domain.sync

interface SmsSyncTrigger {
    suspend fun syncFromProvider()

    fun requestFollowUpSync()
}

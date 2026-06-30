package com.lifelog.service.accessibility

import com.lifelog.domain.model.AccessibilityDebugEvent
import com.lifelog.domain.repository.AccessibilityDebugRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityDebugRepositoryImpl
    @Inject
    constructor(
        private val store: AccessibilityDebugStore,
    ) : AccessibilityDebugRepository {
        override fun observeDebugEvents(): Flow<List<AccessibilityDebugEvent>> = store.entries

        override suspend fun clearDebugEvents() {
            store.clear()
        }
    }

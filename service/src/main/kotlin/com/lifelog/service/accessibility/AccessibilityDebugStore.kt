package com.lifelog.service.accessibility

import com.lifelog.domain.model.AccessibilityDebugEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityDebugStore
    @Inject
    constructor() {
        private val _entries = MutableStateFlow<List<AccessibilityDebugEvent>>(emptyList())
        val entries: StateFlow<List<AccessibilityDebugEvent>> = _entries.asStateFlow()

        fun addEntry(entry: AccessibilityDebugEvent) {
            _entries.value = (listOf(entry) + _entries.value).take(MAX_ENTRIES)
        }

        fun clear() {
            _entries.value = emptyList()
        }

        companion object {
            private const val MAX_ENTRIES = 100
        }
    }

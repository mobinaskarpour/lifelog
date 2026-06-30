package com.lifelog.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.AccessibilityDebugEvent
import com.lifelog.domain.repository.AccessibilityDebugRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesDebugUiState(
    val events: List<AccessibilityDebugEvent> = emptyList(),
)

@HiltViewModel
class MessagesDebugViewModel
    @Inject
    constructor(
        private val accessibilityDebugRepository: AccessibilityDebugRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MessagesDebugUiState())
        val uiState: StateFlow<MessagesDebugUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                accessibilityDebugRepository.observeDebugEvents().collect { events ->
                    _uiState.value = MessagesDebugUiState(events = events)
                }
            }
        }

        fun clearEvents() {
            viewModelScope.launch {
                accessibilityDebugRepository.clearDebugEvents()
            }
        }
    }

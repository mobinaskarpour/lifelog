package com.lifelog.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.repository.TimelineRepository
import com.lifelog.domain.util.TimelineEventSanitizer
import com.lifelog.domain.util.TimelineGrouper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelineUiState(
    val events: List<TimelineEvent> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class TimelineViewModel
    @Inject
    constructor(
        private val timelineRepository: TimelineRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TimelineUiState())
        val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()
        private var loadJob: Job? = null

        init {
            loadEvents()
        }

        fun refresh() {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            loadEvents()
        }

        fun onSearchQueryChange(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            loadEvents()
        }

        private fun loadEvents() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    val query = _uiState.value.searchQuery
                    val flow =
                        if (query.isNotBlank()) {
                            timelineRepository.searchEvents(query)
                        } else {
                            timelineRepository.getAllEvents()
                        }
                    flow
                        .map { events -> processEvents(events) }
                        .catch { error ->
                            _uiState.value =
                                _uiState.value.copy(
                                    events = emptyList(),
                                    isLoading = false,
                                    isRefreshing = false,
                                    errorMessage = error.message,
                                )
                        }.collect { events ->
                            _uiState.value =
                                TimelineUiState(
                                    events = events,
                                    isLoading = false,
                                    isRefreshing = false,
                                    searchQuery = query,
                                )
                        }
                }
        }

        private fun processEvents(events: List<TimelineEvent>): List<TimelineEvent> {
            val sanitized = TimelineEventSanitizer.sanitize(events)
            return TimelineGrouper.mergeAppSessions(sanitized)
        }
    }

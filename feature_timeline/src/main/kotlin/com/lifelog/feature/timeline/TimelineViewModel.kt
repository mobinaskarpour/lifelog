package com.lifelog.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.repository.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelineUiState(
    val events: List<TimelineEvent> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadEvents()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadEvents()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            val flow = if (_uiState.value.searchQuery.isNotBlank()) {
                timelineRepository.searchEvents(_uiState.value.searchQuery)
            } else {
                timelineRepository.getAllEvents()
            }
            flow.catch { }
                .collect { events ->
                    _uiState.value = TimelineUiState(
                        events = events,
                        isLoading = false,
                        isRefreshing = false,
                        searchQuery = _uiState.value.searchQuery,
                    )
                }
        }
    }
}

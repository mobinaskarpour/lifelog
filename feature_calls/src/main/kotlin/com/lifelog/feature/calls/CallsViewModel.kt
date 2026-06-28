package com.lifelog.feature.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.CallLog
import com.lifelog.domain.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallsUiState(
    val calls: List<CallLog> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
)

@HiltViewModel
class CallsViewModel
    @Inject
    constructor(
        private val callRepository: CallRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CallsUiState())
        val uiState: StateFlow<CallsUiState> = _uiState.asStateFlow()

        init {
            loadCalls()
        }

        fun refresh() {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadCalls()
        }

        fun onSearchQueryChange(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            loadCalls()
        }

        private fun loadCalls() {
            viewModelScope.launch {
                val flow =
                    if (_uiState.value.searchQuery.isNotBlank()) {
                        callRepository.searchCalls(_uiState.value.searchQuery)
                    } else {
                        callRepository.getAllCalls()
                    }
                flow.catch { }
                    .collect { calls ->
                        _uiState.value =
                            CallsUiState(
                                calls = calls,
                                isLoading = false,
                                isRefreshing = false,
                                searchQuery = _uiState.value.searchQuery,
                            )
                    }
            }
        }
    }

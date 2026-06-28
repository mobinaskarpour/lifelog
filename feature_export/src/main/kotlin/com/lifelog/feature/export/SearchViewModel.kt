package com.lifelog.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.SearchFilter
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.usecase.SearchLogsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val results: List<TimelineEvent> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchLogsUseCase: SearchLogsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query, isLoading = true)
        viewModelScope.launch {
            searchLogsUseCase(SearchFilter(keyword = query))
                .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
                .collect { results ->
                    _uiState.value = SearchUiState(
                        results = results,
                        query = query,
                        isLoading = false,
                    )
                }
        }
    }
}

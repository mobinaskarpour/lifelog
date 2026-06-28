package com.lifelog.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.DashboardStats
import com.lifelog.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            dashboardRepository.getDashboardStats()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message,
                    )
                }
                .collect { stats ->
                    _uiState.value = DashboardUiState(
                        stats = stats,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
        }
    }
}

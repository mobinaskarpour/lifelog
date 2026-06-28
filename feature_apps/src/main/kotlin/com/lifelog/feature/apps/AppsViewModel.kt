package com.lifelog.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.AppUsage
import com.lifelog.domain.repository.AppUsageRepository
import com.lifelog.utils.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChartPeriod { DAILY, WEEKLY, MONTHLY }

data class AppsUiState(
    val apps: List<AppUsage> = emptyList(),
    val period: ChartPeriod = ChartPeriod.DAILY,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appUsageRepository: AppUsageRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadApps()
    }

    fun setPeriod(period: ChartPeriod) {
        _uiState.value = _uiState.value.copy(period = period)
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val flow = when (_uiState.value.period) {
                ChartPeriod.DAILY -> appUsageRepository.getUsageForDate(DateTimeUtils.formatDate())
                ChartPeriod.WEEKLY -> appUsageRepository.getUsageBetween(
                    DateTimeUtils.daysAgoDate(7),
                    DateTimeUtils.formatDate(),
                )
                ChartPeriod.MONTHLY -> appUsageRepository.getUsageBetween(
                    DateTimeUtils.daysAgoDate(30),
                    DateTimeUtils.formatDate(),
                )
            }
            flow.catch { }
                .collect { apps ->
                    _uiState.value = AppsUiState(
                        apps = apps,
                        period = _uiState.value.period,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
        }
    }
}

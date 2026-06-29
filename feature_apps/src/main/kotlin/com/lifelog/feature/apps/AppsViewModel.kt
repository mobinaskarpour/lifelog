package com.lifelog.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.AppUsage
import com.lifelog.domain.repository.AppUsageRepository
import com.lifelog.utils.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UsagePeriod {
    TODAY,
    YESTERDAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM,
}

enum class AppSortOrder {
    USAGE_TIME,
    LAUNCH_COUNT,
    ALPHABETICAL,
}

data class AppUsageItem(
    val usage: AppUsage,
    val todayPercent: Float,
)

data class AppsUiState(
    val items: List<AppUsageItem> = emptyList(),
    val period: UsagePeriod = UsagePeriod.TODAY,
    val sortOrder: AppSortOrder = AppSortOrder.USAGE_TIME,
    val searchQuery: String = "",
    val customStartDate: String = DateTimeUtils.daysAgoDate(7),
    val customEndDate: String = DateTimeUtils.formatDate(DateTimeUtils.startOfDay()),
    val totalScreenTime: Long = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class AppsViewModel
    @Inject
    constructor(
        private val appUsageRepository: AppUsageRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AppsUiState())
        val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()
        private var loadJob: Job? = null

        init {
            loadApps()
        }

        fun refresh() {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadApps()
        }

        fun setPeriod(period: UsagePeriod) {
            _uiState.value = _uiState.value.copy(period = period)
            loadApps()
        }

        fun setSortOrder(order: AppSortOrder) {
            _uiState.value = _uiState.value.copy(sortOrder = order)
            applySorting()
        }

        fun onSearchQueryChange(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            loadApps()
        }

        fun setCustomDateRange(
            startDate: String,
            endDate: String,
        ) {
            _uiState.value =
                _uiState.value.copy(
                    customStartDate = startDate,
                    customEndDate = endDate,
                    period = UsagePeriod.CUSTOM,
                )
            loadApps()
        }

        private fun loadApps() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    val state = _uiState.value
                    val (startDate, endDate) = dateRangeForPeriod(state)
                    val today = DateTimeUtils.formatDate(DateTimeUtils.startOfDay())

                    val usageFlow =
                        if (startDate == endDate) {
                            appUsageRepository.getUsageForDate(startDate)
                        } else {
                            appUsageRepository.getAggregatedUsageBetween(startDate, endDate)
                        }

                    combine(
                        usageFlow,
                        appUsageRepository.getUsageForDate(today),
                    ) { periodUsage, todayUsage ->
                        val todayTotal = todayUsage.sumOf { it.totalDuration }.coerceAtLeast(1)
                        val todayByPackage = todayUsage.associateBy { it.packageName }
                        val query = state.searchQuery.trim().lowercase()

                        val filtered =
                            periodUsage.filter { app ->
                                query.isBlank() ||
                                    app.appName.lowercase().contains(query) ||
                                    app.packageName.lowercase().contains(query)
                            }

                        val items =
                            filtered.map { app ->
                                val todayDuration = todayByPackage[app.packageName]?.totalDuration ?: 0L
                                AppUsageItem(
                                    usage = app,
                                    todayPercent = todayDuration.toFloat() / todayTotal.toFloat(),
                                )
                            }

                        val sorted = sortItems(items, state.sortOrder)
                        val totalTime = sorted.sumOf { it.usage.totalDuration }

                        AppsUiState(
                            items = sorted,
                            period = state.period,
                            sortOrder = state.sortOrder,
                            searchQuery = state.searchQuery,
                            customStartDate = state.customStartDate,
                            customEndDate = state.customEndDate,
                            totalScreenTime = totalTime,
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }.catch {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                            )
                    }.collect { newState ->
                        _uiState.value = newState
                    }
                }
        }

        private fun applySorting() {
            val state = _uiState.value
            _uiState.value =
                state.copy(
                    items = sortItems(state.items, state.sortOrder),
                )
        }

        private fun sortItems(
            items: List<AppUsageItem>,
            order: AppSortOrder,
        ): List<AppUsageItem> =
            when (order) {
                AppSortOrder.USAGE_TIME -> items.sortedByDescending { it.usage.totalDuration }
                AppSortOrder.LAUNCH_COUNT -> items.sortedByDescending { it.usage.launchCount }
                AppSortOrder.ALPHABETICAL -> items.sortedBy { it.usage.appName.lowercase() }
            }

        private fun dateRangeForPeriod(state: AppsUiState): Pair<String, String> {
            val today = DateTimeUtils.formatDate(DateTimeUtils.startOfDay())
            return when (state.period) {
                UsagePeriod.TODAY -> today to today
                UsagePeriod.YESTERDAY -> DateTimeUtils.yesterdayDate() to DateTimeUtils.yesterdayDate()
                UsagePeriod.LAST_7_DAYS -> DateTimeUtils.daysAgoDate(7) to today
                UsagePeriod.LAST_30_DAYS -> DateTimeUtils.daysAgoDate(30) to today
                UsagePeriod.CUSTOM -> state.customStartDate to state.customEndDate
            }
        }
    }

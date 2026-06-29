package com.lifelog.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.NotificationLog
import com.lifelog.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationGroup(
    val packageName: String,
    val appName: String,
    val notifications: List<NotificationLog>,
)

data class NotificationsUiState(
    val groups: List<NotificationGroup> = emptyList(),
    val availableApps: List<Pair<String, String>> = emptyList(),
    val selectedPackage: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
)

@HiltViewModel
class NotificationsViewModel
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NotificationsUiState())
        val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
        private var loadJob: Job? = null

        init {
            loadNotifications()
        }

        fun refresh() {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadNotifications()
        }

        fun onSearchQueryChange(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            loadNotifications()
        }

        fun setPackageFilter(packageName: String?) {
            _uiState.value = _uiState.value.copy(selectedPackage = packageName)
            loadNotifications()
        }

        private fun loadNotifications() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    val state = _uiState.value
                    val flow =
                        when {
                            state.searchQuery.isNotBlank() ->
                                notificationRepository.searchNotifications(state.searchQuery)
                            state.selectedPackage != null ->
                                notificationRepository.getNotificationsByPackage(state.selectedPackage)
                            else -> notificationRepository.getAllNotifications()
                        }
                    flow.catch {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                            )
                    }.collect { notifications ->
                        val groups =
                            notifications
                                .groupBy { it.packageName }
                                .map { (pkg, items) ->
                                    NotificationGroup(
                                        packageName = pkg,
                                        appName = items.first().appName,
                                        notifications = items.sortedByDescending { it.timestamp },
                                    )
                                }.sortedByDescending { group -> group.notifications.size }

                        val apps =
                            groups.map { group -> group.packageName to group.appName }.distinct()

                        _uiState.value =
                            NotificationsUiState(
                                groups = groups,
                                availableApps = apps,
                                selectedPackage = state.selectedPackage,
                                isLoading = false,
                                isRefreshing = false,
                                searchQuery = state.searchQuery,
                            )
                    }
                }
        }
    }

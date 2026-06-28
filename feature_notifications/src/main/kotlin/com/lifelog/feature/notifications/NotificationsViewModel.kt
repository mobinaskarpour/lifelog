package com.lifelog.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.NotificationLog
import com.lifelog.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<NotificationLog> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

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

    private fun loadNotifications() {
        viewModelScope.launch {
            val flow = if (_uiState.value.searchQuery.isNotBlank()) {
                notificationRepository.searchNotifications(_uiState.value.searchQuery)
            } else {
                notificationRepository.getAllNotifications()
            }
            flow.catch { }
                .collect { notifications ->
                    _uiState.value = NotificationsUiState(
                        notifications = notifications,
                        isLoading = false,
                        isRefreshing = false,
                        searchQuery = _uiState.value.searchQuery,
                    )
                }
        }
    }
}

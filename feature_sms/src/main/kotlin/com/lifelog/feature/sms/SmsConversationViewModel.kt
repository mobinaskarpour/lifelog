package com.lifelog.feature.sms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.SmsMessage
import com.lifelog.domain.repository.SmsRepository
import com.lifelog.domain.sync.SmsSyncTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmsConversationUiState(
    val messages: List<SmsMessage> = emptyList(),
    val threadTitle: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class SmsConversationViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val smsRepository: SmsRepository,
        private val smsSyncTrigger: SmsSyncTrigger,
    ) : ViewModel() {
        private val threadId: Long = savedStateHandle.get<Long>("threadId") ?: 0L

        private val _uiState = MutableStateFlow(SmsConversationUiState())
        val uiState: StateFlow<SmsConversationUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch { smsSyncTrigger.syncFromProvider() }
            loadMessages()
        }

        fun refresh() {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            viewModelScope.launch {
                smsSyncTrigger.syncFromProvider()
            }
            loadMessages()
        }

        private fun loadMessages() {
            viewModelScope.launch {
                smsRepository.getMessagesForThread(threadId)
                    .catch {
                        _uiState.value =
                            SmsConversationUiState(
                                isLoading = false,
                                isRefreshing = false,
                            )
                    }.collect { messages ->
                        val title =
                            messages.firstOrNull()?.contactName
                                ?: messages.firstOrNull()?.address
                                ?: "Conversation"
                        _uiState.value =
                            SmsConversationUiState(
                                messages = messages,
                                threadTitle = title,
                                isLoading = false,
                                isRefreshing = false,
                            )
                    }
            }
        }
    }

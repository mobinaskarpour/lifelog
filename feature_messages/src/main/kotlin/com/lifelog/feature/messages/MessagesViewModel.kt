package com.lifelog.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.MessageChannel
import com.lifelog.domain.model.MessagesOverviewStats
import com.lifelog.domain.model.UniversalConversation
import com.lifelog.domain.repository.MessagesRepository
import com.lifelog.domain.sync.SmsSyncTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val conversations: List<UniversalConversation> = emptyList(),
    val stats: MessagesOverviewStats = MessagesOverviewStats(),
    val selectedChannel: MessageChannel = MessageChannel.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val hasAnyMessages: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MessagesViewModel
    @Inject
    constructor(
        private val messagesRepository: MessagesRepository,
        private val smsSyncTrigger: SmsSyncTrigger,
    ) : ViewModel() {
        private val selectedChannelState = MutableStateFlow(MessageChannel.ALL)
        private val searchQueryState = MutableStateFlow("")
        private val uiStateInternal = MutableStateFlow(MessagesUiState())
        val uiState: StateFlow<MessagesUiState> = uiStateInternal.asStateFlow()

        init {
            viewModelScope.launch { smsSyncTrigger.syncFromProvider() }
            observeData()
        }

        fun onChannelSelected(channel: MessageChannel) {
            selectedChannelState.value = channel
        }

        fun onSearchQueryChange(query: String) {
            searchQueryState.value = query
        }

        private fun observeData() {
            viewModelScope.launch {
                combine(selectedChannelState, searchQueryState) { channel, query ->
                    channel to query
                }.flatMapLatest { (channel, query) ->
                    combine(
                        messagesRepository.observeConversations(channel, query),
                        messagesRepository.observeStats(),
                    ) { conversations, stats ->
                        MessagesUiState(
                            conversations = conversations,
                            stats = stats,
                            selectedChannel = channel,
                            searchQuery = query,
                            isLoading = false,
                            hasAnyMessages = stats.totalMessages > 0,
                        )
                    }
                }.catch {
                    uiStateInternal.value = uiStateInternal.value.copy(isLoading = false)
                }.collect { state ->
                    uiStateInternal.value = state
                }
            }
        }
    }

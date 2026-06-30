package com.lifelog.feature.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.UniversalChatMessage
import com.lifelog.domain.repository.MessagesRepository
import com.lifelog.domain.util.ConversationId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class MessagesConversationUiState(
    val conversationId: String = "",
    val title: String = "Conversation",
    val channelLabel: String = "",
    val packageName: String? = null,
    val messages: List<UniversalChatMessage> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class MessagesConversationViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val messagesRepository: MessagesRepository,
    ) : ViewModel() {
        private val conversationId: String =
            URLDecoder.decode(
                savedStateHandle.get<String>("conversationKey").orEmpty(),
                Charsets.UTF_8.name(),
            )

        private val _uiState = MutableStateFlow(MessagesConversationUiState(conversationId = conversationId))
        val uiState: StateFlow<MessagesConversationUiState> = _uiState.asStateFlow()

        init {
            observeConversation()
        }

        private fun observeConversation() {
            viewModelScope.launch {
                messagesRepository
                    .observeMessages(conversationId)
                    .catch {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }.collect { messages ->
                        val latest = messages.lastOrNull()
                        _uiState.value =
                            MessagesConversationUiState(
                                conversationId = conversationId,
                                title = latest?.sender ?: deriveTitle(conversationId),
                                channelLabel = latest?.channel?.displayName ?: deriveChannel(conversationId),
                                packageName = latest?.packageName,
                                messages = messages,
                                isLoading = false,
                            )
                    }
            }
        }

        private fun deriveTitle(conversationId: String): String {
            ConversationId.parseApp(conversationId)?.let { (_, sender) -> return sender }
            ConversationId.parseSms(conversationId)?.let { return "SMS Thread" }
            return "Conversation"
        }

        private fun deriveChannel(conversationId: String): String {
            if (conversationId.startsWith("sms:")) return "SMS"
            ConversationId.parseApp(conversationId)?.let { (source, _) ->
                return source.replaceFirstChar { it.uppercase() }
            }
            return ""
        }
    }

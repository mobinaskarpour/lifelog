package com.lifelog.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.domain.model.UniversalChatMessage
import com.lifelog.ui.components.AppIcon
import com.lifelog.ui.components.EmptyState
import com.lifelog.ui.components.SkeletonCard
import com.lifelog.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesConversationScreen(
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessagesConversationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatBackground =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                    MaterialTheme.colorScheme.surface,
                ),
        )
    val groupedItems = remember(uiState.messages) { groupMessagesWithDates(uiState.messages) }
    val isGroupChat = uiState.messages.map { it.sender }.distinct().size > 1

    Scaffold(
        modifier = modifier,
        topBar = {
            MessagesTopAppBar(
                title = uiState.title,
                subtitle = uiState.channelLabel,
                onNavigateToSettings = onNavigateToSettings,
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(chatBackground),
        ) {
            if (uiState.isLoading) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(6) { SkeletonCard() }
                }
                return@Box
            }

            if (uiState.messages.isEmpty()) {
                EmptyState(message = "No messages in this conversation yet")
                return@Box
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(
                    items = groupedItems,
                    key = { item ->
                        when (item) {
                            is ChatListItem.DateSeparator -> "date-${item.label}"
                            is ChatListItem.Message -> item.message.id
                        }
                    },
                ) { item ->
                    when (item) {
                        is ChatListItem.DateSeparator ->
                            ChatDateSeparator(label = item.label)
                        is ChatListItem.Message ->
                            UniversalChatBubble(
                                message = item.message,
                                showSender = isGroupChat && !item.message.isOutgoing,
                            )
                    }
                }
            }

            uiState.packageName?.let { packageName ->
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(packageName = packageName, size = 28.dp)
                }
            }
        }
    }
}

private sealed interface ChatListItem {
    data class DateSeparator(val label: String) : ChatListItem

    data class Message(val message: UniversalChatMessage) : ChatListItem
}

private fun groupMessagesWithDates(messages: List<UniversalChatMessage>): List<ChatListItem> {
    if (messages.isEmpty()) return emptyList()
    val result = mutableListOf<ChatListItem>()
    var lastDateLabel: String? = null
    messages.forEach { message ->
        val label = DateTimeUtils.formatChatDateSeparator(message.timestamp)
        if (label != lastDateLabel) {
            result.add(ChatListItem.DateSeparator(label))
            lastDateLabel = label
        }
        result.add(ChatListItem.Message(message))
    }
    return result
}

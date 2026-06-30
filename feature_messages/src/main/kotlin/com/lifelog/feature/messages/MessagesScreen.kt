package com.lifelog.feature.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.ui.components.SkeletonCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onConversationClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenDebug: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: MessagesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            MessagesTopAppBar(
                title = "Messages",
                subtitle = "SMS + Telegram + WhatsApp + Instagram",
                onNavigateToSettings = onNavigateToSettings,
                actions = {
                    if (onOpenDebug != null && com.lifelog.feature.messages.BuildConfig.DEBUG) {
                        androidx.compose.material3.TextButton(onClick = onOpenDebug) {
                            androidx.compose.material3.Text("Debug")
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (uiState.isLoading) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                items(6) { SkeletonCard() }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            item {
                MessagesSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                )
            }
            item {
                MessagesStatsHeader(stats = uiState.stats)
            }
            item {
                MessagesFilterChips(
                    selected = uiState.selectedChannel,
                    onSelected = viewModel::onChannelSelected,
                )
            }

            if (uiState.conversations.isEmpty()) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        if (!uiState.hasAnyMessages && uiState.searchQuery.isBlank()) {
                            MessagesEmptyState(
                                onOpenAccessibility = { openAccessibilitySettings(context) },
                            )
                        } else {
                            com.lifelog.ui.components.EmptyState(
                                message = "No conversations match your search or filter",
                                icon = channelIcon(uiState.selectedChannel),
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.conversations,
                    key = { it.id },
                ) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) },
                    )
                }
            }
        }
    }
}

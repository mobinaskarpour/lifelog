package com.lifelog.feature.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.domain.model.SmsMessage
import com.lifelog.domain.model.SmsMessageType
import com.lifelog.ui.components.EmptyState
import com.lifelog.ui.components.SkeletonCard
import com.lifelog.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsConversationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SmsConversationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.threadTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (uiState.isLoading) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(6) { SkeletonCard() }
                }
                return@PullToRefreshBox
            }

            if (uiState.messages.isEmpty()) {
                EmptyState(message = "No messages in this conversation")
                return@PullToRefreshBox
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.messages,
                    key = { message -> "${message.providerId}_${message.date}" },
                ) { message ->
                    SmsBubble(message = message)
                }
            }
        }
    }
}

@Composable
private fun SmsBubble(message: SmsMessage) {
    val outgoing = message.type.isOutgoing
    val alignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor =
        when (message.type) {
            SmsMessageType.SENT, SmsMessageType.OUTBOX, SmsMessageType.QUEUED ->
                MaterialTheme.colorScheme.primaryContainer
            SmsMessageType.DRAFT -> MaterialTheme.colorScheme.tertiaryContainer
            SmsMessageType.FAILED -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        when (message.type) {
            SmsMessageType.SENT, SmsMessageType.OUTBOX, SmsMessageType.QUEUED ->
                MaterialTheme.colorScheme.onPrimaryContainer
            SmsMessageType.DRAFT -> MaterialTheme.colorScheme.onTertiaryContainer
            SmsMessageType.FAILED -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Surface(
            shape =
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (outgoing) 16.dp else 4.dp,
                    bottomEnd = if (outgoing) 4.dp else 16.dp,
                ),
            color = containerColor,
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = smsTypeLabel(message.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = message.body.ifBlank { "(empty message)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
                Text(
                    text = DateTimeUtils.formatTime(message.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun smsTypeLabel(type: SmsMessageType): String =
    when (type) {
        SmsMessageType.INBOX -> "Incoming"
        SmsMessageType.SENT -> "Outgoing"
        SmsMessageType.OUTBOX -> "Outbox"
        SmsMessageType.DRAFT -> "Draft"
        SmsMessageType.FAILED -> "Failed"
        SmsMessageType.QUEUED -> "Queued"
        SmsMessageType.UNKNOWN -> "Message"
    }

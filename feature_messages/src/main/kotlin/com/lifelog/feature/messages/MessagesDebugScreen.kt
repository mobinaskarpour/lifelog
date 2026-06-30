package com.lifelog.feature.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.domain.model.AccessibilityDebugEvent
import com.lifelog.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesDebugScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessagesDebugViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            MessagesTopAppBar(
                title = "Accessibility Debug",
                subtitle = "Developer only",
                onNavigateToSettings = onBack,
                onBack = onBack,
                actions = {
                    TextButton(onClick = viewModel::clearEvents) {
                        Text("Clear")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.events.isEmpty()) {
                item {
                    Text(
                        text = "No accessibility events captured yet. Open a messaging app to generate debug data.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(uiState.events, key = { it.id }) { event ->
                    DebugEventCard(event)
                }
            }
        }
    }
}

@Composable
private fun DebugEventCard(event: AccessibilityDebugEvent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = event.eventType,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = DateTimeUtils.formatDateTime(event.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DebugLine("Package", event.packageName)
            DebugLine("Sender", event.parsedSender ?: "—")
            DebugLine("Message", event.parsedMessage ?: "—")
            DebugLine("Nodes", event.nodeCount.toString())
            DebugLine("Confidence", event.confidence?.let { "${(it * 100).toInt()}%" } ?: "—")
            DebugLine("Raw nodes", event.rawNodesPreview.ifBlank { "—" })
        }
    }
}

@Composable
private fun DebugLine(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

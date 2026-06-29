package com.lifelog.feature.timeline

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.util.TimelineEventSanitizer
import com.lifelog.ui.components.AppIcon
import com.lifelog.ui.components.EmptyState
import com.lifelog.ui.components.SearchBar
import com.lifelog.ui.components.SkeletonCard
import com.lifelog.ui.components.TimelineItem
import com.lifelog.ui.components.timelineIcon
import com.lifelog.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize(),
    ) {
        if (uiState.isLoading) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(6) { SkeletonCard() }
            }
            return@PullToRefreshBox
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Search timeline...",
                )
            }
            if (uiState.events.isEmpty()) {
                item {
                    EmptyState(
                        message = uiState.errorMessage ?: "No activity recorded yet",
                        modifier = Modifier.padding(top = 64.dp),
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.events,
                    key = { index, event -> TimelineEventSanitizer.stableKey(event, index) },
                ) { _, event ->
                    TimelineEventRow(event = event)
                }
            }
        }
    }
}

@Composable
private fun TimelineEventRow(event: TimelineEvent) {
    val showAppIcon =
        !event.packageName.isNullOrBlank() &&
            event.type in
            setOf(
                TimelineEventType.APP_OPENED,
                TimelineEventType.APP_CLOSED,
                TimelineEventType.WINDOW_CHANGED,
            )
    val title = event.title.ifBlank { "Activity" }
    val subtitle = event.subtitle
    val timestamp = DateTimeUtils.formatTime(event.timestamp)
    val color = timelineColorSafe(event.colorArgb)

    if (showAppIcon) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = event.packageName.orEmpty(), size = 36.dp)
            Spacer(modifier = Modifier.width(4.dp))
            TimelineItem(
                icon = timelineIcon(event.type),
                title = title,
                subtitle = subtitle,
                timestamp = timestamp,
                colorIndicator = color,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        TimelineItem(
            icon = timelineIcon(event.type),
            title = title,
            subtitle = subtitle,
            timestamp = timestamp,
            colorIndicator = color,
        )
    }
}

private fun timelineColorSafe(colorArgb: Long): Color =
    runCatching {
        Color(colorArgb.coerceIn(0L, 0xFFFFFFFFL))
    }.getOrDefault(Color(0xFF6200EE))

package com.lifelog.feature.timeline

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.ui.components.EmptyState
import com.lifelog.ui.components.LoadingState
import com.lifelog.ui.components.SearchBar
import com.lifelog.ui.components.TimelineItem
import com.lifelog.ui.components.timelineColor
import com.lifelog.ui.components.timelineIcon
import com.lifelog.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        LoadingState(modifier)
        return
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize(),
    ) {
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
                        message = "No activity recorded yet",
                        modifier = Modifier.padding(top = 64.dp),
                    )
                }
            } else {
                items(uiState.events, key = { it.id }) { event ->
                    TimelineItem(
                        icon = timelineIcon(event.type),
                        title = event.title,
                        subtitle = event.subtitle,
                        timestamp = DateTimeUtils.formatTime(event.timestamp),
                        colorIndicator = Color(event.colorArgb),
                    )
                }
            }
        }
    }
}

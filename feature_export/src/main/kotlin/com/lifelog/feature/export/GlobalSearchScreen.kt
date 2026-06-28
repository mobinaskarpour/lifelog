package com.lifelog.feature.export

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.ui.components.EmptyState
import com.lifelog.ui.components.SearchBar
import com.lifelog.ui.components.TimelineItem
import com.lifelog.ui.components.timelineColor
import com.lifelog.ui.components.timelineIcon
import com.lifelog.utils.DateTimeUtils

@Composable
fun GlobalSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::search,
                placeholder = "Search all logs...",
            )
        }
        if (uiState.query.isBlank()) {
            item {
                EmptyState("Enter a keyword to search across all logs")
            }
        } else if (uiState.results.isEmpty() && !uiState.isLoading) {
            item {
                EmptyState("No results found for \"${uiState.query}\"")
            }
        } else {
            items(uiState.results, key = { it.id }) { event ->
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

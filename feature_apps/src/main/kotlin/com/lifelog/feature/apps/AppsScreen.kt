package com.lifelog.feature.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.ui.components.AppIcon
import com.lifelog.ui.components.EmptyState
import com.lifelog.ui.components.LinearUsageBar
import com.lifelog.ui.components.SearchBar
import com.lifelog.ui.components.SkeletonCard
import com.lifelog.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Search apps...",
                )
            }
            item {
                LazyRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(UsagePeriod.entries.toList()) { period ->
                        FilterChip(
                            selected = uiState.period == period,
                            onClick = { viewModel.setPeriod(period) },
                            label = {
                                Text(
                                    period.name
                                        .replace('_', ' ')
                                        .lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                )
                            },
                        )
                    }
                }
            }
            item {
                LazyRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(AppSortOrder.entries.toList()) { order ->
                        FilterChip(
                            selected = uiState.sortOrder == order,
                            onClick = { viewModel.setSortOrder(order) },
                            label = {
                                Text(
                                    when (order) {
                                        AppSortOrder.USAGE_TIME -> "Usage"
                                        AppSortOrder.LAUNCH_COUNT -> "Launches"
                                        AppSortOrder.ALPHABETICAL -> "A–Z"
                                    },
                                )
                            },
                        )
                    }
                }
            }
            if (uiState.period == UsagePeriod.CUSTOM) {
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = uiState.customStartDate,
                            onValueChange = { start ->
                                viewModel.setCustomDateRange(start, uiState.customEndDate)
                            },
                            label = { Text("Start") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = uiState.customEndDate,
                            onValueChange = { end ->
                                viewModel.setCustomDateRange(uiState.customStartDate, end)
                            },
                            label = { Text("End") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                }
            }
            if (uiState.items.isNotEmpty()) {
                item {
                    Text(
                        text = "Total: ${DateTimeUtils.formatDuration(uiState.totalScreenTime)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (uiState.items.isEmpty()) {
                item {
                    EmptyState(
                        message = "No app usage data for this period",
                        icon = Icons.Outlined.Apps,
                    )
                }
            } else {
                items(uiState.items, key = { it.usage.packageName }) { item ->
                    val app = item.usage
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(packageName = app.packageName, size = 48.dp)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = DateTimeUtils.formatDuration(app.totalDuration),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "${app.launchCount} launches",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (app.lastOpen > 0) {
                                    Text(
                                        text = "Last opened ${DateTimeUtils.formatRelativeTime(app.lastOpen)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearUsageBar(progress = item.todayPercent)
                                Text(
                                    text = "${(item.todayPercent * 100).toInt()}% of today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

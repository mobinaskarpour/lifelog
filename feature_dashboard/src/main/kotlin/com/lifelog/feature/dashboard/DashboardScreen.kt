package com.lifelog.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.ui.components.LoadingState
import com.lifelog.ui.components.SectionHeader
import com.lifelog.ui.components.StatCard
import com.lifelog.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Today's Overview",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                StatCard(
                    title = "Screen Time",
                    value = DateTimeUtils.formatDuration(uiState.stats.screenTimeMs),
                    icon = Icons.Filled.Timer,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        title = "App Launches",
                        value = uiState.stats.appLaunchCount.toString(),
                        icon = Icons.Filled.TouchApp,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = "Unlocks",
                        value = uiState.stats.unlockCount.toString(),
                        icon = Icons.Filled.LockOpen,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        title = "Notifications",
                        value = uiState.stats.notificationCount.toString(),
                        icon = Icons.Filled.Notifications,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = "Calls",
                        value = uiState.stats.callCount.toString(),
                        icon = Icons.Filled.Phone,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                val batteryText =
                    buildString {
                        append("${uiState.stats.batteryLevel}%")
                        if (uiState.stats.isCharging) append(" ⚡")
                    }
                StatCard(
                    title = "Battery${if (uiState.stats.batteryTemperature > 0) " (${uiState.stats.batteryTemperature}°C)" else ""}",
                    value = batteryText,
                    icon = Icons.Filled.BatteryChargingFull,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            if (uiState.stats.topApps.isNotEmpty()) {
                item { SectionHeader("Top Apps Today") }
                items(uiState.stats.topApps) { app ->
                    StatCard(
                        title = app.appName,
                        value = DateTimeUtils.formatDuration(app.totalDuration),
                        icon = Icons.Filled.Apps,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

package com.lifelog.feature.sms

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.domain.model.SmsAccessStatus
import com.lifelog.ui.components.EmptyState
import com.lifelog.ui.components.SkeletonCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(
    onThreadClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SmsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.refresh()
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            SmsTopAppBar(
                title = "SMS History",
                onNavigateToSettings = onNavigateToSettings,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
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

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    SmsFloatingSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        placeholder = "Search conversations...",
                    )
                }
                item {
                    SmsStatsHeader(stats = uiState.stats)
                }
                if (uiState.accessMessage != null) {
                    item {
                        SmsAccessBanner(
                            message = uiState.accessMessage!!,
                            showPermissionButton = uiState.accessStatus == SmsAccessStatus.PERMISSION_DENIED,
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.READ_SMS)
                            },
                        )
                    }
                } else if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    item {
                        SmsAccessBanner(
                            message = "Grant SMS permission to import inbox and sent messages from this device.",
                            showPermissionButton = true,
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.READ_SMS)
                            },
                        )
                    }
                }
                if (uiState.threads.isEmpty()) {
                    item {
                        EmptyState(
                            message = "No messages yet",
                            icon = Icons.Outlined.Sms,
                        )
                    }
                } else {
                    items(uiState.threads, key = { it.threadId }) { thread ->
                        SmsThreadRow(
                            thread = thread,
                            onClick = { onThreadClick(thread.threadId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsAccessBanner(
    message: String,
    showPermissionButton: Boolean,
    onRequestPermission: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (showPermissionButton) {
                TextButton(onClick = onRequestPermission) {
                    Text("Grant SMS permission")
                }
            }
        }
    }
}

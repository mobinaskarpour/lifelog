package com.lifelog.feature.settings

import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifelog.domain.model.Language
import com.lifelog.domain.model.ThemeMode
import com.lifelog.ui.components.InfoRow
import com.lifelog.ui.components.LoadingState
import com.lifelog.ui.components.SectionHeader
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToAbout: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val exportDir =
        remember {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "lifelog").apply { mkdirs() }
        }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.isLoading) {
        LoadingState(modifier)
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (onBack != null) {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    ),
            )
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            SectionHeader("Appearance")
            SettingsCard {
                ThemeDropdown(
                    selected = uiState.settings.themeMode,
                    onSelected = viewModel::setThemeMode,
                )
                SettingsSwitch(
                    label = "Dynamic Colors",
                    checked = uiState.settings.dynamicColors,
                    onCheckedChange = viewModel::setDynamicColors,
                )
            }

            SectionHeader("Language")
            SettingsCard {
                LanguageDropdown(
                    selected = uiState.settings.language,
                    onSelected = viewModel::setLanguage,
                )
            }

            SectionHeader("Tracking")
            SettingsCard {
                SettingsSwitch(
                    label = "Location Tracking",
                    checked = uiState.settings.locationTrackingEnabled,
                    onCheckedChange = viewModel::setLocationTracking,
                )
                SettingsSwitch(
                    label = "Notification Tracking",
                    checked = uiState.settings.notificationTrackingEnabled,
                    onCheckedChange = viewModel::setNotificationTracking,
                )
            }

            SectionHeader("Data Management")
            SettingsCard {
                AutoDeleteDropdown(
                    selected = uiState.settings.autoDeleteDays,
                    onSelected = viewModel::setAutoDeleteDays,
                )
                Button(
                    onClick = { viewModel.exportCsv(File(exportDir, "lifelog_export.csv").absolutePath) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                ) { Text("Export CSV") }
                Button(
                    onClick = { viewModel.exportJson(File(exportDir, "lifelog_export.json").absolutePath) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                ) { Text("Export JSON") }
                Button(
                    onClick = { viewModel.exportPdf(File(exportDir, "lifelog_export.pdf").absolutePath) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                ) { Text("Export PDF") }
                Button(
                    onClick = { viewModel.backupDatabase(File(exportDir, "lifelog_backup.db").absolutePath) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                ) { Text("Backup Database") }
            }

            SectionHeader("About")
            SettingsCard {
                Button(
                    onClick = onNavigateToAbout,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                ) { Text("About LifeLog") }
                InfoRow(label = "Version", value = "1.0.0")
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
    ) {
        OutlinedTextField(
            value = selected.name.lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Theme") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onSelected(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    selected: Language,
    onSelected: (Language) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
    ) {
        OutlinedTextField(
            value = selected.name.lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Language") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Language.entries.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onSelected(lang)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoDeleteDropdown(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    val options = listOf(0 to "Never", 7 to "7 days", 30 to "30 days", 90 to "90 days")
    var expanded by remember { mutableStateOf(false) }
    val label = options.find { it.first == selected }?.second ?: "Never"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Auto Delete Logs") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (days, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(days)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        Text(
            text = "LifeLog",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text =
                "LifeLog is a personal activity tracker that helps you understand " +
                    "how you use your phone. It records app usage, notifications, calls, " +
                    "screen events, battery status, and network changes — all stored locally on your device.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "Your data never leaves your device unless you explicitly export it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
fun StatisticsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text =
                "View detailed usage statistics across all tracked categories " +
                    "from the Dashboard, Apps, Timeline, and individual log screens.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

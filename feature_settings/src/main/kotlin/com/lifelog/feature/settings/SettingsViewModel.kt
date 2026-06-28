package com.lifelog.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.AppSettings
import com.lifelog.domain.model.Language
import com.lifelog.domain.model.ThemeMode
import com.lifelog.domain.repository.ExportRepository
import com.lifelog.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exportRepository: ExportRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings()
                .catch { }
                .collect { settings ->
                    _uiState.value = SettingsUiState(settings = settings, isLoading = false)
                }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings(_uiState.value.settings.copy(themeMode = mode))
        }
    }

    fun setLanguage(language: Language) {
        viewModelScope.launch {
            settingsRepository.updateSettings(_uiState.value.settings.copy(language = language))
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(_uiState.value.settings.copy(dynamicColors = enabled))
        }
    }

    fun setAutoDeleteDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings(_uiState.value.settings.copy(autoDeleteDays = days))
        }
    }

    fun setLocationTracking(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                _uiState.value.settings.copy(locationTrackingEnabled = enabled),
            )
        }
    }

    fun setNotificationTracking(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                _uiState.value.settings.copy(notificationTrackingEnabled = enabled),
            )
        }
    }

    fun exportCsv(outputPath: String) {
        viewModelScope.launch {
            val content = exportRepository.exportToCsv()
            java.io.File(outputPath).writeText(content)
            _uiState.value = _uiState.value.copy(message = "CSV exported successfully")
        }
    }

    fun exportJson(outputPath: String) {
        viewModelScope.launch {
            val content = exportRepository.exportToJson()
            java.io.File(outputPath).writeText(content)
            _uiState.value = _uiState.value.copy(message = "JSON exported successfully")
        }
    }

    fun exportPdf(outputPath: String) {
        viewModelScope.launch {
            val success = exportRepository.exportToPdf(outputPath)
            _uiState.value = _uiState.value.copy(
                message = if (success) "PDF exported successfully" else "PDF export failed",
            )
        }
    }

    fun backupDatabase(outputPath: String) {
        viewModelScope.launch {
            val success = exportRepository.backupDatabase(outputPath)
            _uiState.value = _uiState.value.copy(
                message = if (success) "Backup created" else "Backup failed",
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

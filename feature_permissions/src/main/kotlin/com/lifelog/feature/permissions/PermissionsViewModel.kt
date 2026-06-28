package com.lifelog.feature.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionsUiState(
    val permissions: List<PermissionItem> = emptyList(),
    val allGranted: Boolean = false,
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val items = PermissionHelper.getPermissionItems(context)
        _uiState.value = PermissionsUiState(
            permissions = items,
            allGranted = PermissionHelper.allRequiredGranted(context),
        )
    }

    fun openSettings(permissionId: String) {
        PermissionHelper.openPermissionSettings(context, permissionId)
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            onComplete()
        }
    }
}

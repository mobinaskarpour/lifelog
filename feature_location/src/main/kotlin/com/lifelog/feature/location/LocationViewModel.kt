package com.lifelog.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.LocationLog
import com.lifelog.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationUiState(
    val locations: List<LocationLog> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    init {
        loadLocations()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadLocations()
    }

    private fun loadLocations() {
        viewModelScope.launch {
            locationRepository.getAllLocations()
                .catch { }
                .collect { locations ->
                    _uiState.value = LocationUiState(
                        locations = locations,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
        }
    }
}

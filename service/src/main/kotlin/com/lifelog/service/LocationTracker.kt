package com.lifelog.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.lifelog.domain.model.LocationLog
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.repository.LocationRepository
import com.lifelog.domain.repository.SettingsRepository
import com.lifelog.domain.repository.TimelineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val timelineRepository: TimelineRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { saveLocation(it) }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        scope.launch {
            val settings = settingsRepository.getSettings().first()
            if (!settings.locationTrackingEnabled) return@launch

            try {
                val intervalMs = settings.locationIntervalMinutes * 60_000L
                val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
                    .setMinUpdateIntervalMillis(intervalMs)
                    .build()
                fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                isTracking = true
                Timber.d("Location tracking started")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start location tracking")
            }
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        fusedClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    private fun saveLocation(location: Location) {
        scope.launch {
            try {
                locationRepository.insertLocation(
                    LocationLog(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = System.currentTimeMillis(),
                    ),
                )
                timelineRepository.insertEvent(
                    TimelineEvent(
                        type = TimelineEventType.LOCATION_UPDATE,
                        title = "Location Update",
                        subtitle = "${location.latitude}, ${location.longitude}",
                        timestamp = System.currentTimeMillis(),
                        colorArgb = 0xFF4CAF50,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Error saving location")
            }
        }
    }
}

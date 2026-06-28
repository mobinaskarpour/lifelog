package com.lifelog.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.view.accessibility.AccessibilityEvent
import com.lifelog.domain.model.ScreenEvent
import com.lifelog.domain.model.ScreenEventType
import com.lifelog.domain.model.TimelineEvent
import com.lifelog.domain.model.TimelineEventType
import com.lifelog.domain.repository.ScreenEventRepository
import com.lifelog.domain.repository.TimelineRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LifeLogAccessibilityService : AccessibilityService() {
    @Inject lateinit var screenEventRepository: ScreenEventRepository

    @Inject lateinit var timelineRepository: TimelineRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wasKeyguardLocked = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        wasKeyguardLocked = isKeyguardLocked()
        Timber.d("LifeLog accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            -> checkUnlockState()
        }
    }

    override fun onInterrupt() {
        Timber.d("LifeLog accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("LifeLog accessibility service destroyed")
    }

    private fun checkUnlockState() {
        val isLocked = isKeyguardLocked()
        if (wasKeyguardLocked && !isLocked) {
            logDeviceUnlock()
        }
        wasKeyguardLocked = isLocked
    }

    private fun isKeyguardLocked(): Boolean {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isKeyguardLocked
    }

    private fun logDeviceUnlock() {
        val timestamp = System.currentTimeMillis()
        scope.launch {
            try {
                screenEventRepository.insertScreenEvent(
                    ScreenEvent(
                        type = ScreenEventType.DEVICE_UNLOCK,
                        timestamp = timestamp,
                    ),
                )
                timelineRepository.insertEvent(
                    TimelineEvent(
                        type = TimelineEventType.PHONE_UNLOCKED,
                        title = "Phone Unlocked",
                        subtitle = "Device unlocked",
                        timestamp = timestamp,
                        colorArgb = 0xFF4CAF50,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Error logging device unlock from accessibility service")
            }
        }
    }
}

package com.lifelog.service.worker

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifelog.domain.usecase.CleanupOldLogsUseCase
import com.lifelog.domain.repository.SettingsRepository
import com.lifelog.service.LifeLogTrackingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cleanupOldLogsUseCase: CleanupOldLogsUseCase,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = settingsRepository.getSettings().first()
        if (settings.autoDeleteDays > 0) {
            cleanupOldLogsUseCase(settings.autoDeleteDays)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "lifelog_cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}

object ServiceStarter {
    fun startTracking(context: Context) {
        val intent = Intent(context, LifeLogTrackingService::class.java)
        context.startForegroundService(intent)
        CleanupWorker.schedule(context)
    }
}

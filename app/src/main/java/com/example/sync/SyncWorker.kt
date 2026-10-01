package com.example.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppContainer
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AppContainer.initialize(applicationContext)
            val result = AppContainer.syncManager.sync()
            when (result) {
                is SyncResult.Success -> Result.success()
                is SyncResult.NotConfigured -> Result.success()
                is SyncResult.SignedOut -> Result.success()
                is SyncResult.Offline -> Result.retry()
                is SyncResult.Error -> Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_SYNC_WORK_NAME = "life_manager_periodic_sync"
        private const val ONE_TIME_SYNC_WORK_NAME = "life_manager_one_time_sync"
        private const val CHANGE_SYNC_WORK_NAME = "life_manager_change_sync"

        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
            } catch (_: Exception) {}
        }

        fun triggerImmediateSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    ONE_TIME_SYNC_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeRequest
                )
            } catch (_: Exception) {}
        }

        /**
         * Called after any local data change. Waits a few seconds (debounce) so a burst of
         * edits results in a single sync instead of one sync per edit.
         */
        fun triggerDebouncedSync(context: Context, delaySeconds: Long = 5) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    CHANGE_SYNC_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (_: Exception) {}
        }
    }
}

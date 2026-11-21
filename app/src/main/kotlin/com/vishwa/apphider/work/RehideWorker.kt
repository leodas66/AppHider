package com.vishwa.apphider.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vishwa.apphider.AppHiderApplication

class RehideWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as AppHiderApplication).container
        var failed = false
        container.hiddenAppDao.getAll()
            .filter { it.shouldBeHidden }
            .forEach { app ->
                if (container.root.hide(app.packageName).isSuccess) {
                    // Clear the "temporarily visible" marker so the UI and notification stop
                    // reporting this app as unhidden.
                    container.hiddenAppDao.upsert(app.copy(lastUnlockedAt = 0L))
                } else {
                    failed = true
                }
            }

        val stillUnhidden = container.hiddenAppDao.getAll()
            .count { it.shouldBeHidden && it.lastUnlockedAt != 0L }
        if (stillUnhidden == 0) {
            container.notifications.dismiss()
        } else {
            container.notifications.show(stillUnhidden)
        }

        return when {
            !failed -> Result.success()
            runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}

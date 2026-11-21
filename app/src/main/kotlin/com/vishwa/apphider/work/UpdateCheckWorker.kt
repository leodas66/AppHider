package com.vishwa.apphider.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vishwa.apphider.AppHiderApplication

class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val container = (applicationContext as AppHiderApplication).container
        val info = container.updates.check()
        if (info.versionCode > container.updates.currentVersionCode()) {
            container.notifications.showUpdateAvailable(info.versionName)
        }
        Result.success()
    } catch (e: Exception) {
        Result.failure()
    }
}

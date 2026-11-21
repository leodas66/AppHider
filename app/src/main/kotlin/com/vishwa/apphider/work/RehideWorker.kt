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
        container.rehide.rehideAll()
        return Result.success()
    }
}

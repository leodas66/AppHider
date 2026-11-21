package com.vishwa.apphider

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.topjohnwu.superuser.Shell
import com.vishwa.apphider.boot.ScreenOffReceiver
import com.vishwa.apphider.work.UpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AppHiderApplication : Application() {

    lateinit var container: AppContainer
        private set

    companion object {
        private const val UPDATE_CHECK_WORK = "ota_update_check"

        init {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(Shell.Builder.create())
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannel()
        container.disguise.apply()
        registerReceiver(ScreenOffReceiver(), IntentFilter(Intent.ACTION_SCREEN_OFF))
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.rehide.reconcile()
        }
        scheduleUpdateCheck()
    }

    private fun scheduleUpdateCheck() {
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UPDATE_CHECK_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

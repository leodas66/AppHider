package com.vishwa.apphider.rehide

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ServiceCompat
import com.vishwa.apphider.AppHiderApplication
import com.vishwa.apphider.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Watches the foreground app while a hidden app is temporarily visible, then re-hides it. */
class RehideWatchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (watchJob?.isActive != true) watchJob = scope.launch { watch() }
        return START_STICKY
    }

    override fun onDestroy() {
        watchJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val notification = container.notifications.watcherNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                Notifications.WATCHER_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(Notifications.WATCHER_NOTIFICATION_ID, notification)
        }
    }

    private suspend fun watch() {
        val dao = container.hiddenAppDao
        var activePackages = emptySet<String>()
        var seenPackages = emptySet<String>()

        while (scope.isActive) {
            val rows = dao.getAll().filter { it.shouldBeHidden && it.lastUnlockedAt != 0L }
            if (rows.isEmpty()) {
                stopWatcher()
                return
            }

            val packages = rows.map { it.packageName }.toSet()
            if (packages != activePackages) {
                activePackages = packages
                seenPackages = emptySet()
            }

            val foreground = container.root.foregroundPackage()
            if (foreground != null && foreground in packages) {
                seenPackages = seenPackages + foreground
            } else if (foreground != null && seenPackages.isNotEmpty()) {
                container.rehide.rehideAll()
                return
            }

            delay(POLL_INTERVAL_MS)
        }
    }

    private fun stopWatcher() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val container get() = (applicationContext as AppHiderApplication).container

    private companion object {
        const val POLL_INTERVAL_MS = 1_000L
    }
}

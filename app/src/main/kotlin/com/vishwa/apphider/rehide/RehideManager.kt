package com.vishwa.apphider.rehide

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import com.vishwa.apphider.AppContainer
import com.vishwa.apphider.work.RehideWorker
import java.util.concurrent.TimeUnit

/** Single owner for re-hide state: Room markers, notification, timer and foreground watcher. */
class RehideManager(
    private val context: Context,
    private val container: AppContainer,
) {

    suspend fun rehideAll() {
        var failed = false
        container.hiddenAppDao.getAll()
            .filter { it.shouldBeHidden }
            .forEach { app ->
                if (container.root.hide(app.packageName).isSuccess) {
                    container.hiddenAppDao.upsert(app.copy(lastUnlockedAt = 0L))
                } else {
                    failed = true
                }
            }
        if (failed) container.notifications.showRehideError()
        syncState()
    }

    /** Brings Room, the device and the old `pm hide` format into agreement. */
    suspend fun reconcile() {
        val known = container.root.listAll().toSet()
        val installed = container.root.listInstalled().toSet()

        container.hiddenAppDao.getAll().forEach { app ->
            val packageName = app.packageName
            when {
                packageName !in known -> container.hiddenAppDao.delete(app)

                app.shouldBeHidden && (packageName in installed || container.root.isLegacyHidden(packageName)) -> {
                    if (container.root.hide(packageName).isSuccess) {
                        container.hiddenAppDao.upsert(app.copy(lastUnlockedAt = 0L))
                    }
                }

                !app.shouldBeHidden && (packageName !in installed || container.root.isLegacyHidden(packageName)) -> {
                    if (container.root.unhide(packageName).isSuccess) {
                        container.hiddenAppDao.upsert(app.copy(lastUnlockedAt = 0L))
                    }
                }

                app.lastUnlockedAt != 0L -> {
                    container.hiddenAppDao.upsert(app.copy(lastUnlockedAt = 0L))
                }
            }
        }
        syncState()
    }

    suspend fun syncState(startWatcher: Boolean = false) {
        val unhiddenCount = container.hiddenAppDao.getAll()
            .count { it.shouldBeHidden && it.lastUnlockedAt != 0L }

        if (unhiddenCount == 0) {
            container.work.cancelUniqueWork(REHIDE_WORK)
            container.notifications.dismiss()
            stopWatcher()
            return
        }

        container.notifications.show(unhiddenCount)
        if (container.settings.autoRehideMinutes > 0) {
            scheduleRehide()
        } else {
            container.work.cancelUniqueWork(REHIDE_WORK)
        }
        if (startWatcher) startWatcher()
    }

    fun startWatcher() {
        try {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, RehideWatchService::class.java),
            )
        } catch (_: Exception) {
            // A background-start restriction must not break the app launch; the timeout and
            // screen-off re-hide paths remain available.
        }
    }

    fun stopWatcher() {
        runCatching {
            context.applicationContext.stopService(
                Intent(context.applicationContext, RehideWatchService::class.java),
            )
        }
    }

    private fun scheduleRehide() {
        val request = OneTimeWorkRequestBuilder<RehideWorker>()
            .setInitialDelay(
                container.settings.autoRehideMinutes.toLong(),
                TimeUnit.MINUTES,
            )
            .build()
        container.work.enqueueUniqueWork(REHIDE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val REHIDE_WORK = "rehide"
    }
}

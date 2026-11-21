package com.vishwa.apphider.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vishwa.apphider.AppHiderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val container = (context.applicationContext as AppHiderApplication).container
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val installed = container.root.listAll().toSet()
                container.hiddenAppDao.getAll().forEach { app ->
                    if (app.packageName !in installed) {
                        container.hiddenAppDao.delete(app)
                        return@forEach
                    }
                    val actuallyHidden = container.root.isHidden(app.packageName)
                    if (app.shouldBeHidden && !actuallyHidden) {
                        container.root.hide(app.packageName)
                    } else if (!app.shouldBeHidden && actuallyHidden) {
                        container.root.unhide(app.packageName)
                    }
                    if (app.lastUnlockedAt != null) {
                        container.hiddenAppDao.upsert(app.copy(lastUnlockedAt = 0L))
                    }
                }
                container.notifications.dismiss()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

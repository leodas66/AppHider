package com.vishwa.apphider.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vishwa.apphider.AppHiderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HideAllReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val pendingResult = goAsync()
        val container = (context.applicationContext as AppHiderApplication).container
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                container.hiddenAppDao.getAll()
                    .filter { it.shouldBeHidden }
                    .forEach { app ->
                        if (container.root.hide(app.packageName).isSuccess) {
                            container.hiddenAppDao.upsert(app.copy(lastUnlockedAt = 0L))
                        }
                    }
                container.work.cancelUniqueWork(REHIDE_WORK)
                container.notifications.dismiss()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.vishwa.apphider.HIDE_ALL"
        const val REHIDE_WORK = "rehide"
    }
}

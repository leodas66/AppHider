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
                container.hiddenAppDao.getAll().forEach { container.root.hide(it.packageName) }
                container.work.cancelAllWork()
                container.notifications.dismiss()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.vishwa.apphider.HIDE_ALL"
    }
}

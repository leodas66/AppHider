package com.vishwa.apphider

import android.app.Application
import com.topjohnwu.superuser.Shell

class AppHiderApplication : Application() {

    lateinit var container: AppContainer
        private set

    companion object {
        init {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR))
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannel()
    }
}

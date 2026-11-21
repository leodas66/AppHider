package com.vishwa.apphider

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.vishwa.apphider.data.AppDatabase
import com.vishwa.apphider.data.HiddenAppDao
import com.vishwa.apphider.data.PackageRepository
import com.vishwa.apphider.data.SettingsStore
import com.vishwa.apphider.disguise.DisguiseManager
import com.vishwa.apphider.lock.LockManager
import com.vishwa.apphider.lock.PinStore
import com.vishwa.apphider.notifications.Notifications
import com.vishwa.apphider.rehide.RehideManager
import com.vishwa.apphider.root.RootManager
import com.vishwa.apphider.update.UpdateInstaller
import com.vishwa.apphider.update.UpdateRepository

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "apphider.db").build()
    }

    val hiddenAppDao: HiddenAppDao by lazy { database.hiddenAppDao() }
    val root: RootManager by lazy { RootManager() }
    val pin: PinStore by lazy { PinStore(context.applicationContext) }
    val lock: LockManager by lazy { LockManager() }
    val packages: PackageRepository by lazy { PackageRepository(context.applicationContext) }
    val settings: SettingsStore by lazy { SettingsStore(context.applicationContext) }
    val work: WorkManager by lazy { WorkManager.getInstance(context.applicationContext) }
    val notifications: Notifications by lazy { Notifications(context.applicationContext) }
    val updates: UpdateRepository by lazy { UpdateRepository(context.applicationContext) }
    val updateInstaller: UpdateInstaller by lazy { UpdateInstaller(context.applicationContext) }
    val rehide: RehideManager by lazy { RehideManager(context.applicationContext, this) }
    val disguise: DisguiseManager by lazy { DisguiseManager(context.applicationContext, settings) }
}

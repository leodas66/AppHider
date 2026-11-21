package com.vishwa.apphider.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HiddenApp::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hiddenAppDao(): HiddenAppDao
}

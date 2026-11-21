package com.vishwa.apphider.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_apps")
data class HiddenApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val shouldBeHidden: Boolean = true,
    val lastUnlockedAt: Long = 0L,
)

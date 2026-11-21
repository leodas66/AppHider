package com.vishwa.apphider.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenAppDao {

    @Query("SELECT * FROM hidden_apps WHERE shouldBeHidden = 1 ORDER BY label COLLATE NOCASE")
    fun observeTracked(): Flow<List<HiddenApp>>

    @Query("SELECT * FROM hidden_apps")
    suspend fun getAll(): List<HiddenApp>

    @Query("SELECT * FROM hidden_apps WHERE packageName = :packageName")
    suspend fun get(packageName: String): HiddenApp?

    @Upsert
    suspend fun upsert(app: HiddenApp)

    @Delete
    suspend fun delete(app: HiddenApp)
}

package com.vishwa.apphider.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HiddenAppDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: HiddenAppDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.hiddenAppDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `tracked list excludes permanently unhidden apps`() = runBlocking {
        dao.upsert(HiddenApp("com.example.hidden", "Hidden"))
        dao.upsert(HiddenApp("com.example.visible", "Visible", shouldBeHidden = false))
        dao.upsert(HiddenApp("com.example.launched", "Launched", lastUnlockedAt = 123L))

        val tracked = dao.observeTracked().first().map { it.packageName }

        assertEquals(listOf("com.example.hidden", "com.example.launched"), tracked)
    }

    @Test
    fun `unhiding permanently removes the app from the tracked list`() = runBlocking {
        dao.upsert(HiddenApp("com.example.app", "App", lastUnlockedAt = 42L))
        dao.upsert(HiddenApp("com.example.app", "App", shouldBeHidden = false, lastUnlockedAt = 0L))

        assertEquals(emptyList<String>(), dao.observeTracked().first().map { it.packageName })
        val row = dao.get("com.example.app")
        assertEquals(false, row?.shouldBeHidden)
        assertEquals(0L, row?.lastUnlockedAt)
    }

    @Test
    fun `temporarily unhidden apps stay tracked with their timestamp`() = runBlocking {
        dao.upsert(HiddenApp("com.example.app", "App", lastUnlockedAt = 42L))

        val tracked = dao.observeTracked().first().single()

        assertEquals("com.example.app", tracked.packageName)
        assertEquals(42L, tracked.lastUnlockedAt)
    }
}

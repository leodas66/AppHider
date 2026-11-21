package com.vishwa.apphider.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `defaults to 5 minutes and biometric enabled`() {
        val store = SettingsStore(context)
        assertEquals(5, store.autoRehideMinutes)
        assertTrue(store.biometricEnabled)
    }

    @Test
    fun `screen off rehide is represented by non-positive timeout`() {
        val store = SettingsStore(context)
        store.autoRehideMinutes = 0
        assertTrue(store.isScreenOffRehide)
        store.autoRehideMinutes = 1
        assertFalse(store.isScreenOffRehide)
    }
}

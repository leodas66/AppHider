package com.vishwa.apphider.lock

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `hasPin is false with empty prefs`() {
        val store = PinStore(context)
        assertFalse(store.hasPin())
        assertFalse(store.verify("1234"))
    }

    @Test
    fun `setPin rejects invalid formats`() {
        val store = PinStore(context)
        assertThrows(IllegalArgumentException::class.java) { store.setPin("123") }
        assertThrows(IllegalArgumentException::class.java) { store.setPin("1234567") }
        assertThrows(IllegalArgumentException::class.java) { store.setPin("12ab") }
        assertFalse(store.hasPin())
    }
}

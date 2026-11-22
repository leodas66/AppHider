package com.vishwa.apphider.root

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RootManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val root = RootManager(context)

    @Test
    fun `invalid package names are rejected before shell command`() {
        assertThrows(IllegalArgumentException::class.java) { root.hide("bad package") }
        assertThrows(IllegalArgumentException::class.java) { root.unhide("../etc") }
        assertThrows(IllegalArgumentException::class.java) { root.isHidden("com") }
    }
}

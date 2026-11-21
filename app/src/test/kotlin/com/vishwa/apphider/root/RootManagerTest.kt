package com.vishwa.apphider.root

import org.junit.Assert.assertThrows
import org.junit.Test

class RootManagerTest {

    private val root = RootManager()

    @Test
    fun `invalid package names are rejected before shell command`() {
        assertThrows(IllegalArgumentException::class.java) { root.hide("bad package") }
        assertThrows(IllegalArgumentException::class.java) { root.unhide("../etc") }
        assertThrows(IllegalArgumentException::class.java) { root.isHidden("com") }
    }
}

package com.vishwa.apphider.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PmOutputTest {

    @Test
    fun `parses hidden state reported by pm`() {
        assertTrue(PmOutput.hiddenStateFrom("Package com.example new hidden state: true") == true)
        assertTrue(PmOutput.hiddenStateFrom("Package com.example new hidden state: false") == false)
        assertNull(PmOutput.hiddenStateFrom("Success"))
    }

    @Test
    fun `prefers hidden flag for the current user`() {
        val dump = listOf(
            "  User 0: 12345",
            "  User 10: com.example installed=true hidden=false enabled=true",
            "  User 0: com.example installed=true hidden=true enabled=true",
        )

        assertTrue(PmOutput.isHiddenForUser(dump, 0))
        assertFalse(PmOutput.isHiddenForUser(dump, 10))
    }

    @Test
    fun `parses hidden flag from a real pm dump line`() {
        val dump = listOf(
            "    User 0: ceDataInode=123 deDataInode=456 installed=true hidden=true suspended=false",
        )
        assertTrue(PmOutput.isHiddenForUser(dump, 0))
    }

    @Test
    fun `parses package lists with and without uninstalled packages`() {
        val lines = listOf(
            "package:com.android.shell",
            "package:com.example.hidden",
            "",
        )
        assertEquals(setOf("com.android.shell", "com.example.hidden"), PmOutput.packagesFromList(lines))
    }

    @Test
    fun `parses focus lines from window and activity dumps`() {
        assertEquals(
            "com.example.app",
            PmOutput.packageFromFocusLine(
                "    mCurrentFocus=Window{abc u0 com.example.app/.MainActivity}",
            ),
        )
        assertEquals(
            "org.example.other",
            PmOutput.packageFromFocusLine(
                "  topResumedActivity=ActivityRecord{abc u0 org.example.other/.Main t4}",
            ),
        )
        assertEquals(
            "com.pipe.app",
            PmOutput.packageFromFocusLine("u0|com.pipe.app/.Main"),
        )
        assertNull(PmOutput.packageFromFocusLine("mCurrentFocus=null"))
    }
}

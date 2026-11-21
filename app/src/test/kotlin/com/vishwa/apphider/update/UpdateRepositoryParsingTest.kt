package com.vishwa.apphider.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateRepositoryParsingTest {

    @Test
    fun `parseManifest reads version and apk urls`() {
        val json = """
            {
              "versionCode": 12,
              "versionName": "1.2.0",
              "releaseNotes": "Fixes",
              "apk": {
                "arm64-v8a": "https://example.com/a64.apk",
                "universal": "https://example.com/u.apk"
              }
            }
        """.trimIndent()

        val info = parseManifest(json)

        assertEquals(12, info.versionCode)
        assertEquals("1.2.0", info.versionName)
        assertEquals("Fixes", info.releaseNotes)
        assertEquals("https://example.com/a64.apk", info.apkUrls["arm64-v8a"])
        assertEquals("https://example.com/u.apk", info.apkUrls["universal"])
    }

    @Test
    fun `selectApkUrl prefers supported abi then universal`() {
        val info = UpdateInfo(
            versionCode = 1,
            versionName = "1.0",
            releaseNotes = "",
            apkUrls = mapOf(
                "arm64-v8a" to "https://example.com/a64.apk",
                "universal" to "https://example.com/u.apk",
            ),
        )

        assertEquals("https://example.com/a64.apk", selectApkUrl(info, listOf("x86_64", "arm64-v8a")))
        assertEquals("https://example.com/u.apk", selectApkUrl(info, listOf("x86")))
    }

    @Test
    fun `selectApkUrl returns null when no match`() {
        val info = UpdateInfo(1, "1.0", "", mapOf("armeabi-v7a" to "https://example.com/a7.apk"))
        assertNull(selectApkUrl(info, listOf("x86_64")))
    }
}

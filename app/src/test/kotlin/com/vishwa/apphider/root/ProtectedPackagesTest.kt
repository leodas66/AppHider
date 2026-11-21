package com.vishwa.apphider.root

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedPackagesTest {

    @Test
    fun `own package is always protected`() {
        assertTrue(ProtectedPackages.isProtected("com.vishwa.apphider", "com.vishwa.apphider"))
    }

    @Test
    fun `critical system packages are protected`() {
        assertTrue(ProtectedPackages.isProtected("com.android.systemui", "com.vishwa.apphider"))
        assertTrue(ProtectedPackages.isProtected("com.android.settings", "com.vishwa.apphider"))
        assertTrue(ProtectedPackages.isProtected("com.google.android.gms", "com.vishwa.apphider"))
        assertTrue(ProtectedPackages.isProtected("com.android.shell", "com.vishwa.apphider"))
    }

    @Test
    fun `protected package prefixes are protected`() {
        assertTrue(ProtectedPackages.isProtected("com.android.settings.SubSettings", "com.vishwa.apphider"))
    }

    @Test
    fun `regular apps are not protected`() {
        assertFalse(ProtectedPackages.isProtected("com.example.app", "com.vishwa.apphider"))
        assertFalse(ProtectedPackages.isProtected("com.android.chrome", "com.vishwa.apphider"))
    }
}

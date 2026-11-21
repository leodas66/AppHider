package com.vishwa.apphider.root

object ProtectedPackages {

    private val ALWAYS = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.gms",
        "com.android.launcher3",
        "com.google.android.googlequicksearchbox",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.shell",
        "com.android.providers.settings",
    )

    fun isProtected(packageName: String, ownPackage: String): Boolean =
        packageName == ownPackage || ALWAYS.any { packageName.startsWith(it) }
}

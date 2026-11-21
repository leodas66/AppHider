package com.vishwa.apphider.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class PackageRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun labelOf(packageName: String): String = infoOrNull(packageName)?.label ?: packageName

    fun iconOf(packageName: String): Drawable? = infoOrNull(packageName)?.icon

    fun appInfo(packageName: String, protected: Boolean): AppInfo =
        (infoOrNull(packageName) ?: AppInfo(packageName, packageName, false)).copy(protected = protected)

    /**
     * Looks up an installed or hidden package. Returns null when the package is unknown or its
     * resources cannot be loaded, so callers can fall back to a stored label instead of crashing.
     */
    fun infoOrNull(packageName: String): AppInfo? {
        val info = runCatching {
            packageManager.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        }.getOrNull() ?: return null
        return AppInfo(
            packageName = packageName,
            label = runCatching { packageManager.getApplicationLabel(info).toString() }
                .getOrDefault(packageName),
            protected = false,
            icon = runCatching { info.loadIcon(packageManager) }.getOrNull(),
        )
    }

    fun launch(packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}

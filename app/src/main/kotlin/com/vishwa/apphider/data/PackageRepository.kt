package com.vishwa.apphider.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class PackageRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun labelOf(packageName: String): String = infoOf(packageName).label

    fun iconOf(packageName: String): Drawable? = infoOf(packageName).icon

    fun appInfo(packageName: String, protected: Boolean): AppInfo {
        val info = infoOf(packageName)
        return AppInfo(
            packageName = packageName,
            label = info.label,
            protected = protected,
            icon = info.icon,
        )
    }

    fun launch(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun infoOf(packageName: String): AppInfo = try {
        val info = packageManager.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        AppInfo(
            packageName = packageName,
            label = packageManager.getApplicationLabel(info).toString(),
            protected = false,
            icon = info.loadIcon(packageManager),
        )
    } catch (e: PackageManager.NameNotFoundException) {
        AppInfo(packageName = packageName, label = packageName, protected = false)
    }
}

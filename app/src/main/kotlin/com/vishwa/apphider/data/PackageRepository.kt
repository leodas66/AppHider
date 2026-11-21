package com.vishwa.apphider.data

import android.content.Context
import android.content.pm.PackageManager

class PackageRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun labelOf(packageName: String): String = try {
        val info = packageManager.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }

    fun launch(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

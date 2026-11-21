package com.vishwa.apphider.disguise

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.vishwa.apphider.data.SettingsStore

/** Switches the launcher identity between App Hider and a calculator. */
class DisguiseManager(
    private val context: Context,
    private val settings: SettingsStore,
) {

    val isEnabled: Boolean get() = settings.disguised

    fun setEnabled(enabled: Boolean) {
        settings.disguised = enabled
        apply()
    }

    /** Re-applies the stored choice after an update or a fresh process start. */
    fun apply() {
        setAlias(NORMAL_ALIAS, !isEnabled)
        setAlias(CALCULATOR_ALIAS, isEnabled)
    }

    private fun setAlias(name: String, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context.packageName, name),
            state,
            PackageManager.DONT_KILL_APP,
        )
    }

    companion object {
        const val NORMAL_ALIAS = "com.vishwa.apphider.NormalLauncherAlias"
        const val CALCULATOR_ALIAS = "com.vishwa.apphider.CalculatorLauncherAlias"
        const val SECRET_CODE = "2774437"
    }
}

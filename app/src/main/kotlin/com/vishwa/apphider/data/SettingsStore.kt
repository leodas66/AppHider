package com.vishwa.apphider.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var autoRehideMinutes: Int
        get() = prefs.getInt(KEY_TIMEOUT, 5)
        set(value) = prefs.edit().putInt(KEY_TIMEOUT, value).apply()

    val isScreenOffRehide: Boolean
        get() = autoRehideMinutes <= 0

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    private companion object {
        const val KEY_TIMEOUT = "auto_rehide_minutes"
        const val KEY_BIOMETRIC = "biometric_enabled"
    }
}

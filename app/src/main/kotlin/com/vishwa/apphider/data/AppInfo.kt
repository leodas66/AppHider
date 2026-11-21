package com.vishwa.apphider.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val protected: Boolean,
    val icon: Drawable? = null,
    val unhidden: Boolean = false,
)

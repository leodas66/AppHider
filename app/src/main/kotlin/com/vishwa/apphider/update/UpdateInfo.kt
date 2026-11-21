package com.vishwa.apphider.update

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val apkUrls: Map<String, String>,
)

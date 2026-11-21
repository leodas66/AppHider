package com.vishwa.apphider.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.io.use

class UpdateRepository(private val context: Context) {

    private val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }

    @Suppress("DEPRECATION")
    fun currentVersionCode(): Int {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else info.versionCode
    }

    fun currentVersionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        parseManifest(fetchText(MANIFEST_URL))
    }

    suspend fun download(info: UpdateInfo, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val url = selectApkUrl(info) ?: throw IllegalStateException("No APK for this device ABI")
        require(url.startsWith("https://", ignoreCase = true)) { "Update URL must use HTTPS" }

        val apk = File(updateDir, "app-update-${info.versionCode}.apk")
        if (apk.exists()) apk.delete()

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Download failed (HTTP $code)")

            val total = connection.contentLength
            connection.inputStream.use { input ->
                FileOutputStream(apk).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val progress = if (total > 0) (downloaded * 100 / total).coerceIn(0, 100) else -1
                        onProgress(progress)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        if (apk.length() == 0L) {
            apk.delete()
            throw IllegalStateException("Downloaded APK is empty")
        }
        verifyApk(apk)
        apk
    }

    private fun fetchText(urlString: String): String {
        require(urlString.startsWith("https://", ignoreCase = true)) { "Manifest URL must use HTTPS" }
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Update check failed (HTTP $code)")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun selectApkUrl(info: UpdateInfo): String? = selectApkUrl(info, Build.SUPPORTED_ABIS.toList())

    @Suppress("DEPRECATION")
    private fun verifyApk(apk: File) {
        val packageManager = context.packageManager
        val archive = packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)
            ?: throw SecurityException("Downloaded APK could not be read")
        if (archive.packageName != context.packageName) {
            apk.delete()
            throw SecurityException("Downloaded APK has a different package name")
        }

        val installed = packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        val installedSignatures = installed.signatures?.map { it.toByteArray() }
        val archiveSignatures = archive.signatures?.map { it.toByteArray() }
        if (!signaturesMatch(installedSignatures, archiveSignatures)) {
            apk.delete()
            throw SecurityException("Downloaded APK signature does not match")
        }
    }

    private companion object {
        const val MANIFEST_URL = "https://github.com/leodas66/AppHider/releases/latest/download/ota.json"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 60_000
    }
}

internal fun signaturesMatch(installed: List<ByteArray>?, archive: List<ByteArray>?): Boolean {
    if (installed.isNullOrEmpty() || archive.isNullOrEmpty()) return false
    if (installed.size != archive.size) return false
    return installed.zip(archive).all { (a, b) -> MessageDigest.isEqual(a, b) }
}

internal fun parseManifest(json: String): UpdateInfo {
    val root = JSONObject(json)
    val apkObject = root.getJSONObject("apk")
    val urls = mutableMapOf<String, String>()
    val keys = apkObject.keys()
    while (keys.hasNext()) {
        val abi = keys.next()
        urls[abi] = apkObject.getString(abi)
    }
    return UpdateInfo(
        versionCode = root.getInt("versionCode"),
        versionName = root.getString("versionName"),
        releaseNotes = root.optString("releaseNotes"),
        apkUrls = urls,
    )
}

internal fun selectApkUrl(info: UpdateInfo, supportedAbis: List<String>): String? {
    supportedAbis.forEach { abi ->
        info.apkUrls[abi]?.let { return it }
    }
    return info.apkUrls["universal"]
}

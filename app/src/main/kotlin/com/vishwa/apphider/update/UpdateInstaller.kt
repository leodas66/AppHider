package com.vishwa.apphider.update

import android.content.Context
import com.topjohnwu.superuser.Shell
import java.io.File

class UpdateInstaller(private val context: Context) {

    fun install(apk: File): Result<Unit> {
        if (!apk.exists()) return Result.failure(IllegalArgumentException("Update file is missing"))
        if (!apk.absolutePath.startsWith(context.cacheDir.absolutePath)) {
            return Result.failure(SecurityException("Update file is outside the app cache"))
        }

        val quotedPath = "'" + apk.absolutePath.replace("'", "'\\''") + "'"
        val result = Shell.cmd("pm install -r -d $quotedPath").exec()
        return if (result.isSuccess) {
            apk.delete()
            Result.success(Unit)
        } else {
            Result.failure(RuntimeException(result.err.joinToString("\n").ifBlank { result.out.joinToString("\n") }))
        }
    }
}

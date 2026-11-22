package com.vishwa.apphider.root

import android.content.Context
import android.os.Process
import com.topjohnwu.superuser.Shell
import java.io.File

class RootManager(private val context: Context) {

    fun checkRoot(onResult: (Boolean) -> Unit) {
        Shell.getShell { shell -> onResult(shell.isRoot) }
    }

    /**
     * Hides an app by uninstalling it for the current user while keeping its APK and data. This is
     * deliberately stronger than `pm hide`: hidden apps no longer appear in Settings > Apps.
     *
     * `pm uninstall -k` deletes the APK of user-installed apps on many ROMs, so the APKs are copied
     * into this app's private files first. Without that backup `pm install-existing` cannot restore
     * such an app later.
     */
    fun hide(packageName: String): Result<Unit> {
        validate(packageName)
        if (!listAll().contains(packageName)) {
            return Result.failure(IllegalStateException("Package is not installed on this device"))
        }
        // Migrate apps hidden by an older version with `pm hide` to the strong-hide format. A
        // legacy-hidden package is not reported by a normal `pm list packages`, so hiding it again
        // without clearing that flag leaves it half-hidden and impossible to launch.
        if (isLegacyHidden(packageName)) {
            val cleared = unhideLegacy(packageName)
            if (cleared.isFailure) return cleared
        }
        if (!isInstalled(packageName)) {
            return Result.failure(IllegalStateException("Package is not installed for the current user"))
        }

        val codePath = Shell.cmd("pm dump $packageName").exec().out
            .firstNotNullOfOrNull(PmOutput::codePathFrom)
        if (codePath == null || !codePath.startsWith("/data/app/") || !codePath.contains(packageName)) {
            return Result.failure(IllegalStateException("Could not locate the APK of $packageName"))
        }

        val backup = backupDir(packageName)
        val copy = Shell.cmd(
            "mkdir -p '${backup.absolutePath}' && cp '${codePath}'/*.apk '${backup.absolutePath}/'",
        ).exec()
        if (!copy.isSuccess) {
            deleteBackup(packageName)
            return Result.failure(commandFailure(copy.out + copy.err))
        }

        val result = Shell.cmd("pm uninstall -k --user ${currentUserId()} $packageName").exec()
        if (!result.isSuccess) {
            deleteBackup(packageName)
            return Result.failure(commandFailure(result.out + result.err))
        }

        return if (!isInstalled(packageName) && listAll().contains(packageName)) {
            Result.success(Unit)
        } else {
            deleteBackup(packageName)
            Result.failure(IllegalStateException("Package is still installed after hide"))
        }
    }

    /** Restores a hidden app for the current user with its data intact. */
    fun unhide(packageName: String): Result<Unit> {
        validate(packageName)
        // `pm hide` leaves the package installed, so `install-existing` would fail on it.
        if (isLegacyHidden(packageName)) return unhideLegacy(packageName)
        if (isInstalled(packageName)) return Result.success(Unit)
        if (!listAll().contains(packageName)) {
            return Result.failure(IllegalStateException("Package is not installed on this device"))
        }

        var result = Shell.cmd("pm install-existing --user ${currentUserId()} --wait $packageName").exec()
        var restored = isInstalled(packageName)

        // System apps can be reinstalled from their system APK, but user-installed apps need the
        // backup that was made when they were hidden.
        if (!restored) {
            val apks = backupApks(packageName)
            if (apks.isEmpty()) {
                return Result.failure(
                    IllegalStateException("The APK of $packageName is missing. Reinstall the app, then hide it again."),
                )
            }
            val paths = apks.joinToString(" ") { "'${it.absolutePath}'" }
            result = Shell.cmd("pm install -r -d --user ${currentUserId()} $paths").exec()
            restored = isInstalled(packageName)
        }

        if (!restored) return Result.failure(commandFailure(result.out + result.err))
        deleteBackup(packageName)
        return Result.success(Unit)
    }

    /** True when the package is known to the system but not installed for the current user. */
    fun isHidden(packageName: String): Boolean {
        validate(packageName)
        return listAll().contains(packageName) && !isInstalled(packageName)
    }

    /** Detects the legacy `pm hide` flag so older installs can be migrated. */
    fun isLegacyHidden(packageName: String): Boolean {
        validate(packageName)
        val dump = Shell.cmd("pm dump $packageName").exec().out
        return PmOutput.isHiddenForUser(dump, currentUserId())
    }

    /** Clears the legacy `pm hide` flag for apps that are meant to be visible. */
    fun unhideLegacy(packageName: String): Result<Unit> {
        validate(packageName)
        val result = Shell.cmd("pm unhide --user ${currentUserId()} $packageName").exec()
        if (!result.isSuccess) return Result.failure(commandFailure(result.out + result.err))
        val reported = result.out.firstNotNullOfOrNull(PmOutput::hiddenStateFrom)
        if (reported == true || isLegacyHidden(packageName)) {
            return Result.failure(IllegalStateException("Package is still hidden"))
        }
        return Result.success(Unit)
    }

    /**
     * Best-effort foreground package lookup using root `dumpsys`. Avoids hidden APIs and the
     * PACKAGE_USAGE_STATS permission that `addOnUidImportanceListener` requires.
     */
    fun foregroundPackage(): String? {
        for (command in FOCUS_COMMANDS) {
            val output = Shell.cmd(command).exec().out
            output.firstNotNullOfOrNull(PmOutput::packageFromFocusLine)?.let { return it }
        }
        return null
    }

    /** Includes packages that are hidden or uninstalled for the current user. */
    fun listAll(): List<String> =
        packageList("pm list packages -u --user ${currentUserId()}")

    /** Only packages installed for the current user. */
    fun listInstalled(): List<String> =
        packageList("pm list packages --user ${currentUserId()}")

    private fun isInstalled(packageName: String): Boolean =
        listInstalled().contains(packageName)

    private fun packageList(command: String): List<String> =
        PmOutput.packagesFromList(Shell.cmd(command).exec().out).sorted()

    private fun backupDir(packageName: String): File =
        File(File(context.applicationContext.filesDir, "hidden-apks"), packageName)

    private fun backupApks(packageName: String): List<File> =
        backupDir(packageName).listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }
            .sortedWith(compareBy({ it.name != "base.apk" }, { it.name }))

    private fun deleteBackup(packageName: String) {
        Shell.cmd("rm -rf '${backupDir(packageName).absolutePath}'").exec()
    }

    private fun commandFailure(lines: List<String>): RuntimeException =
        RuntimeException(lines.joinToString("\n").ifBlank { "Shell command failed" })

    /** Android encodes the user id in the upper bits of a UID. */
    private fun currentUserId(): Int = Process.myUid() / USER_ID_RANGE

    private fun validate(packageName: String) {
        require(PACKAGE_NAME.matches(packageName)) { "Invalid package name: $packageName" }
    }

    private companion object {
        const val USER_ID_RANGE = 100_000
        val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
        val FOCUS_COMMANDS = listOf(
            "dumpsys window windows | grep -m1 mCurrentFocus",
            "dumpsys activity activities | grep -m1 topResumedActivity",
            "dumpsys activity activities | grep -m1 mResumedActivity",
        )
    }
}

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
     * Hides an app the way Amarok does: disable it and set the legacy `pm hide` flag. On this ROM
     * `pm hide` alone still leaves the app in Settings > Apps, but combining it with `pm disable`
     * removes it. The APK and data stay in place, so no backup or reinstall is needed.
     */
    fun hide(packageName: String): Result<Unit> {
        validate(packageName)
        if (!isKnown(packageName)) {
            return Result.failure(IllegalStateException("Package is not installed on this device"))
        }
        if (isVisible(packageName)) {
            val result = Shell.cmd("pm disable $packageName && pm hide $packageName").exec()
            if (!result.isSuccess) return Result.failure(commandFailure(result.out + result.err))
            return if (!isVisible(packageName)) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Package is still visible after hide"))
            }
        }

        // Hidden by an older strong-hide release: nothing to uninstall, but it must be disabled so
        // it does not reappear in Settings.
        if (isLegacyHidden(packageName)) {
            val result = Shell.cmd("pm disable $packageName").exec()
            if (!result.isSuccess) return Result.failure(commandFailure(result.out + result.err))
        }
        return Result.success(Unit)
    }

    /** Restores a hidden app for the current user with its data intact. */
    fun unhide(packageName: String): Result<Unit> {
        validate(packageName)
        if (!isKnown(packageName)) {
            return Result.failure(IllegalStateException("Package is not installed on this device"))
        }
        if (isVisible(packageName) && !isLegacyHidden(packageName)) return Result.success(Unit)

        var result = Shell.cmd("pm unhide $packageName; pm enable $packageName").exec()
        if (isVisible(packageName)) return Result.success(Unit)

        // Strong-hidden apps from older releases need their APK restored first.
        result = Shell.cmd("pm install-existing --user ${currentUserId()} --wait $packageName").exec()
        if (isVisible(packageName)) return Result.success(Unit)

        val apks = backupApks(packageName)
        if (apks.isEmpty()) {
            return Result.failure(
                IllegalStateException("The APK of $packageName is missing. Reinstall the app, then hide it again."),
            )
        }
        val paths = apks.joinToString(" ") { "'${it.absolutePath}'" }
        result = Shell.cmd("pm install -r -d --user ${currentUserId()} $paths").exec()
        if (!isVisible(packageName)) return Result.failure(commandFailure(result.out + result.err))
        deleteBackup(packageName)
        return Result.success(Unit)
    }

    /** True when the package is known to the system but not visible for the current user. */
    fun isHidden(packageName: String): Boolean {
        validate(packageName)
        return isKnown(packageName) && !isVisible(packageName)
    }

    /** Detects the legacy `pm hide` flag so older installs can be migrated. */
    fun isLegacyHidden(packageName: String): Boolean {
        validate(packageName)
        val dump = Shell.cmd("pm dump $packageName").exec().out
        return PmOutput.isHiddenForUser(dump, currentUserId())
    }

    /** Clears the legacy `pm hide` flag for apps that are meant to be visible. */
    fun unhideLegacy(packageName: String): Result<Unit> = unhide(packageName)

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

    /** Filtered lookup is much cheaper than listing every package on the device. */
    private fun isVisible(packageName: String): Boolean =
        packageList("pm list packages --user ${currentUserId()} $packageName").contains(packageName)

    private fun isKnown(packageName: String): Boolean =
        packageList("pm list packages -u --user ${currentUserId()} $packageName").contains(packageName)

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

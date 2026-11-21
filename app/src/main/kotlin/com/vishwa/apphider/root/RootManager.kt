package com.vishwa.apphider.root

import android.os.Process
import com.topjohnwu.superuser.Shell

class RootManager {

    fun checkRoot(onResult: (Boolean) -> Unit) {
        Shell.getShell { shell -> onResult(shell.isRoot) }
    }

    /**
     * Hides an app by uninstalling it for the current user while keeping its APK and data. This is
     * deliberately stronger than `pm hide`: hidden apps no longer appear in Settings > Apps.
     */
    fun hide(packageName: String): Result<Unit> {
        validate(packageName)
        if (!listAll().contains(packageName)) {
            return Result.failure(IllegalStateException("Package is not installed on this device"))
        }
        if (!isInstalled(packageName)) return Result.success(Unit)

        val result = Shell.cmd("pm uninstall -k --user ${currentUserId()} $packageName").exec()
        if (!result.isSuccess) return Result.failure(commandFailure(result.out + result.err))

        return if (!isInstalled(packageName) && listAll().contains(packageName)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Package is still installed after hide"))
        }
    }

    /** Restores a hidden app for the current user with its data intact. */
    fun unhide(packageName: String): Result<Unit> {
        validate(packageName)
        if (isInstalled(packageName)) return Result.success(Unit)
        if (!listAll().contains(packageName)) {
            return Result.failure(IllegalStateException("Package is not installed on this device"))
        }

        val result = Shell.cmd("pm install-existing --user ${currentUserId()} --wait $packageName").exec()
        if (!result.isSuccess) return Result.failure(commandFailure(result.out + result.err))

        return if (isInstalled(packageName)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Package was not restored after unhide"))
        }
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
        if (reported == true) return Result.failure(IllegalStateException("Package is still hidden"))
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

package com.vishwa.apphider.root

import com.topjohnwu.superuser.Shell

class RootManager {

    fun checkRoot(onResult: (Boolean) -> Unit) {
        Shell.getShell { shell -> onResult(shell.isRoot) }
    }

    fun hide(packageName: String): Result<Unit> = runPm("pm hide", packageName)

    fun unhide(packageName: String): Result<Unit> = runPm("pm unhide", packageName)

    fun isHidden(packageName: String): Boolean {
        validate(packageName)
        return Shell.cmd("pm dump $packageName").exec().out.any { it.trim() == "hidden=true" }
    }

    fun listAll(): List<String> =
        Shell.cmd("pm list packages -u").exec().out
            .mapNotNull { it.removePrefix("package:").takeIf(String::isNotBlank) }
            .sorted()

    private fun runPm(command: String, packageName: String): Result<Unit> {
        validate(packageName)
        val result = Shell.cmd("$command $packageName").exec()
        return if (result.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(RuntimeException(result.err.joinToString("\n")))
        }
    }

    private fun validate(packageName: String) {
        require(PACKAGE_NAME.matches(packageName)) { "Invalid package name: $packageName" }
    }

    private companion object {
        val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    }
}

package com.vishwa.apphider.root

/** Pure parsers for `pm` / `dumpsys` output. Kept separate so they can be unit tested. */
internal object PmOutput {

    private val HIDDEN_STATE = Regex("""new hidden state:\s*(true|false)""", RegexOption.IGNORE_CASE)
    private val USER_HIDDEN = Regex("""User\s+(\d+):.*?\bhidden=(true|false)\b""")
    private val ANY_HIDDEN = Regex("""\bhidden=(true|false)\b""")
    private val PACKAGE_LINE = Regex("""^package:(\S+)$""")
    private val FOCUSED_PACKAGE =
        Regex("""\bu\d+\s*\|?\s*([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+)/""")

    /** Legacy `pm hide` / `pm unhide` print the state reported by the package manager. */
    fun hiddenStateFrom(line: String): Boolean? =
        HIDDEN_STATE.find(line)?.groupValues?.get(1)?.equals("true", ignoreCase = true)

    /** Parses the `hidden=true` field from a legacy `pm dump` for the given user. */
    fun isHiddenForUser(lines: List<String>, userId: Int): Boolean {
        lines.firstNotNullOfOrNull { line ->
            val match = USER_HIDDEN.find(line) ?: return@firstNotNullOfOrNull null
            if (match.groupValues[1].toIntOrNull() != userId) return@firstNotNullOfOrNull null
            match.groupValues[2].equals("true", ignoreCase = true)
        }?.let { return it }

        return lines.firstNotNullOfOrNull { line ->
            ANY_HIDDEN.find(line)?.groupValues?.get(1)?.equals("true", ignoreCase = true)
        } ?: false
    }

    /** Parses `pm list packages`, including uninstalled packages when `-u` was used. */
    fun packagesFromList(lines: List<String>): Set<String> =
        lines.mapNotNull { PACKAGE_LINE.find(it)?.groupValues?.get(1) }.toSet()

    /** Extracts a package name from `dumpsys window` / `dumpsys activity` focus lines. */
    fun packageFromFocusLine(line: String): String? =
        FOCUSED_PACKAGE.find(line)?.groupValues?.get(1)
}

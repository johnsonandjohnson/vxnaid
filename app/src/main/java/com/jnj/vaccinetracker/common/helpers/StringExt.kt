package com.jnj.vaccinetracker.common.helpers

private const val NON_THIN = "[^iIl1\\.,']"
private fun textWidth(str: String): Int {
    return str.length - str.replace(NON_THIN.toRegex(), "").length / 2
}

fun String.ellipsize(max: Int): String {
    val text = this

    if (text.length <= max) {
        return text
    }

    if (textWidth(text) <= max)
        return text

    // Start by chopping off at the word before max
    // This is an over-approximation due to thin-characters...
    var end = text.lastIndexOf(' ', max - 3)

    // Just one long word. Chop it off.
    if (end == -1)
        return text.substring(0, max - 3) + "..."

    // Step forward as long as textWidth allows.
    var newEnd = end
    do {
        end = newEnd
        newEnd = text.indexOf(' ', end + 1)

        // No more spaces.
        if (newEnd == -1)
            newEnd = text.length

    } while (textWidth(text.substring(0, newEnd) + "...") < max)

    return text.substring(0, end) + "..."
}

fun String.toLower() = lowercase()
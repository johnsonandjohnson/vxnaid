package com.jnj.vaccinetracker.common.helpers

/**
 * note that [requireNotNull] does **not** throw [NullPointerException]
 */
fun Throwable.isFatalException() = when (this) {
    is UnsupportedOperationException,
    is NotImplementedError,
    is ConcurrentModificationException,
    is NullPointerException,
    -> true
    else -> false
}

fun Throwable.rethrowIfFatal() = if (isFatalException()) throw this else this


fun Throwable.buildStackTraceString(): String = stackTraceToString()


fun Throwable.findMessage(messagePart: String): Boolean = message?.contains(messagePart)?.takeIf { it } ?: cause?.findMessage(messagePart) ?: false

inline fun <reified T> runCatchingDbQuery(block: () -> T): T? {
    return try {
        block()
    } catch (ex: IllegalStateException) {
        ex.rethrowIfFatal()
        if (ex.message?.contains("already closed") == true || ex.message == "Attempting to open already closed database.") {
            // java.lang.IllegalStateException: database /data/user/0/com.jnj.vaccinetracker/databases/participants.db already closed
            "runCatchingDbQuery".logWarn("couldn't perform database query because database is closed likely due to P2P transfer", ex)
            null
        } else {
            throw ex
        }
    }
}

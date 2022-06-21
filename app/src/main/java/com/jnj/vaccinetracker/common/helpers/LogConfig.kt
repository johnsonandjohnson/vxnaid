package com.jnj.vaccinetracker.common.helpers

sealed class LogTarget {
    abstract val logPriority: LogPriority

    data class Console(override val logPriority: LogPriority) : LogTarget()
    data class File(override val logPriority: LogPriority) : LogTarget()

    fun isEnabledFor(logPriority: LogPriority): Boolean {
        return logPriority >= this.logPriority
    }
}

data class LogConfig(val targets: List<LogTarget>) {
    val hasTargets get() = targets.isNotEmpty()
    val consoleTarget get() = targets.filterIsInstance<LogTarget.Console>().firstOrNull()
    val fileTarget get() = targets.filterIsInstance<LogTarget.File>().firstOrNull()
    val consoleLoggingEnabled get() = consoleTarget != null
    val fileLoggingEnabled get() = fileTarget != null

    fun isFileLoggingEnabled(logPriority: LogPriority): Boolean = fileTarget?.isEnabledFor(logPriority) ?: false
    fun isConsoleLoggingEnabled(logPriority: LogPriority): Boolean = consoleTarget?.isEnabledFor(logPriority) ?: false
    fun isAnyLoggingEnabled(logPriority: LogPriority): Boolean = targets.any { it.isEnabledFor(logPriority) }

    companion object {
        val NONE = LogConfig(emptyList())
        val DEFAULT = LogConfig(listOf(LogTarget.Console(LogPriority.PRINTLN), LogTarget.File(LogPriority.INFO)))
    }
}
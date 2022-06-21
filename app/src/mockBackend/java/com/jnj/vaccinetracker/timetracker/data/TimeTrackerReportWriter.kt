package com.jnj.vaccinetracker.timetracker.data

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.timetracker.domain.entities.TimeLeap
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TimeTrackerReportWriter @Inject constructor(private val timeTrackerReportFileProvider: TimeTrackerReportFileProvider, private val dispatchers: AppCoroutineDispatchers) {

    private val reportFile by lazy { timeTrackerReportFileProvider.provideTimeTrackerReportFile() }

    private companion object {
        private const val SEP = ","
    }

    private fun appendLine(line: String) {
        reportFile.appendText(line + System.lineSeparator())
    }

    private fun initFile() {
        if (reportFile.length() == 0L) {
            appendLine(TimeLeap.Column.defaultList().joinToString(SEP) { it.toDisplay() })
        }
    }

    suspend fun appendTimeLeap(timeLeap: TimeLeap) = withContext(dispatchers.io) {
        initFile()
        appendLine(timeLeap.toStringList().joinToString(SEP))
    }
}
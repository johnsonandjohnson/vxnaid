package com.jnj.vaccinetracker.timetracker.data.datasources

import com.tfcporciuncula.flow.FlowSharedPreferences
import javax.inject.Inject

class LastTimestampAddedToReportDataSource @Inject constructor(private val prefs: FlowSharedPreferences) {

    private companion object {
        private const val PREF_LAST_TIMESTAMP_ADDED_TO_REPORT = "last_timestamp_added_to_report"
    }

    private val lastTimestampAddedToReport get() = prefs.getLong(PREF_LAST_TIMESTAMP_ADDED_TO_REPORT)

    fun storeLastTimestamp(timestamp: Long) = lastTimestampAddedToReport.set(timestamp)
    fun getLastTimestamp() = lastTimestampAddedToReport.get()
}
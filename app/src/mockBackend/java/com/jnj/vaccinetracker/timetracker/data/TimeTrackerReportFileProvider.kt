package com.jnj.vaccinetracker.timetracker.data

import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import java.io.File
import javax.inject.Inject

class TimeTrackerReportFileProvider @Inject constructor(private val filesDirProvider: AndroidFiles) {
    companion object {
        private const val FILE_NAME = "time_tracker_report.csv"
    }

    fun provideTimeTrackerReportFile() = File(filesDirProvider.externalFiles, FILE_NAME)
}
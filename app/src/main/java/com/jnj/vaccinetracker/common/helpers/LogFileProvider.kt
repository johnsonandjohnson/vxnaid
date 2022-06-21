package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.ui.minus
import com.jnj.vaccinetracker.common.ui.plus
import com.jnj.vaccinetracker.config.appSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class LogFileProvider @Inject constructor(private val filesDirProvider: AndroidFiles) {
    companion object {
        private const val LOG_FILE_EXT = ".log"
        private const val LOG_FILE_NAME_PREFIX = "vmp_log"
        private const val LOG_FOLDER = "logs"
        private val logNameDateFormat = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
        private fun buildLogFileName(date: String) = "${LOG_FILE_NAME_PREFIX}_${appVersion}_${appSettings.flavor}_$date$LOG_FILE_EXT"
        private fun buildLogFileName(date: Date): String = buildLogFileName(logNameDateFormat.format(date))
        private fun buildLogFileName(dateA: Date, dateB: Date) = buildLogFileName("${logNameDateFormat.format(dateA)}-${logNameDateFormat.format(dateB)}")
        private val CHECK_STALE_LOGS_INTERVAL = 1.hours
    }

    private val mutex = Mutex()
    private var dateLastCheckedStaleLogs: Date? = null

    private fun shouldDeleteStaleLogs() = dateLastCheckedStaleLogs?.let { lastDate ->
        lastDate + CHECK_STALE_LOGS_INTERVAL < Date()
    } ?: true

    private suspend fun removeStaleLogsIfNeeded() {
        mutex.withLock {
            if (shouldDeleteStaleLogs()) {
                removeStaleLogs()
            }
        }
    }

    fun deleteAll() {
        logFileFolder.deleteChildren()
    }

    private fun File.addLinesTo(writer: BufferedWriter) {
        if (exists() && canRead()) {
            bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    writer.appendLine(line)
                }
            }
        }
    }

    suspend fun createJoinedLogFile(coroutineContext: CoroutineContext = Dispatchers.IO): File = withContext(coroutineContext) {
        mutex.withLock {
            val dateUntil = Date()
            val dateFrom = dateUntil - 1.days
            val yesterday = buildLogFile(dateFrom)
            val today = buildLogFile(dateUntil)
            val joined = buildLogFile(dateFrom, dateUntil)
            joined.bufferedWriter().use { writer ->
                yesterday.addLinesTo(writer)
                today.addLinesTo(writer)
            }
            joined
        }
    }

    suspend fun provideCurrentLogFile(): File {
        removeStaleLogsIfNeeded()
        return buildLogFile(Date())
    }

    private val logFileFolder: File by lazy {
        val folder = File(filesDirProvider.externalFiles, LOG_FOLDER)
        folder.mkdir()
        folder
    }

    private fun listLogFiles(): List<File> {
        return logFileFolder.listFiles { f -> f.name.endsWith(LOG_FILE_EXT) }.orEmpty().toList()
    }

    private fun buildLogFile(date: Date) = File(logFileFolder, buildLogFileName(date))
    private fun buildLogFile(dateA: Date, dateB: Date) = File(logFileFolder, buildLogFileName(dateA, dateB))

    private fun calcDaysAllowed() = listOf(Date(), Date() - 1.days)

    private fun removeStaleLogs() {
        val filesAllowed = calcDaysAllowed().map { buildLogFile(it) }
        listLogFiles().forEach { f ->
            if (f !in filesAllowed) {
                f.delete()
            }
        }
        dateLastCheckedStaleLogs = Date()
    }
}
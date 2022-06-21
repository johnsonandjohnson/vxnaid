package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.config.appSettings
import com.jnj.vaccinetracker.sync.domain.usecases.error.BuildSyncErrorDeviceInfoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LogWriter @Inject constructor(
    private val buildSyncErrorDeviceInfoUseCase: BuildSyncErrorDeviceInfoUseCase,
    private val logFileProvider: LogFileProvider,
    private val dispatchers: AppCoroutineDispatchers,
) {

    companion object {

        private val logLineDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.ENGLISH)
    }

    private val job = SupervisorJob()

    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private val mutex by lazy {
        Mutex()
    }

    fun writeLog(logPriority: LogPriority, tag: String, part: String) {
        require(appSettings.logConfig.isFileLoggingEnabled(logPriority)) { "called writeLog while file logging is not enabled for $logPriority" }
        scope.launch(Dispatchers.IO) {
            val date = Date()
            val text = "${logLineDateFormat.format(date)} / ${logPriority.abbreviation} / $tag -- $part"
            mutex.withLock {
                writeText(logFile = logFileProvider.provideCurrentLogFile(), text)
            }
        }
    }

    private fun writeText(logFile: File, text: String) {
        var logFileCreated = false
        try {
            if (!logFile.exists()) {
                logFile.parentFile!!.mkdirs()
                logFile.createNewFile()
                logFileCreated = true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            BufferedWriter(FileWriter(logFile, true)).use { buf ->
                if (logFileCreated) {
                    buf.append(buildAppVersionTag())
                    buf.newLine()
                    buf.append(buildSyncErrorDeviceInfoUseCase.build().toString())
                    buf.newLine()
                }
                buf.append(text)
                buf.newLine()

            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun buildAppVersionTag(): String {
        return "${appVersion}_${appSettings.flavor}"
    }
}
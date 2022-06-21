package com.jnj.vaccinetracker.sync.data.io

import com.jnj.vaccinetracker.common.data.database.mappers.SyncErrorJsonMapper
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.models.SyncErrorsDto
import com.jnj.vaccinetracker.sync.data.models.toDto
import com.jnj.vaccinetracker.sync.domain.entities.SyncError
import com.jnj.vaccinetracker.sync.domain.usecases.error.BuildSyncErrorDeviceInfoUseCase
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.File
import javax.inject.Inject

class SyncErrorFileWriter @Inject constructor(
    private val androidFiles: AndroidFiles,
    private val syncErrorMetadataJsonMapper: SyncErrorJsonMapper,
    private val buildSyncErrorDeviceInfoUseCase: BuildSyncErrorDeviceInfoUseCase,
    private val dispatchers: AppCoroutineDispatchers,
) {

    companion object {
        private const val ERROR_LOG_FOLDER = "sync_error_logs"
    }

    private fun createLogFolder(): File {
        val filesDir = androidFiles.externalFiles
        val folder = File(filesDir, ERROR_LOG_FOLDER)
        folder.mkdirs()
        return folder
    }

    private fun createLogFile(): File {
        return File(createLogFolder(), buildFileName())
    }

    private fun buildFileName(): String {
        return "sync_errors.json"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun writeAll(syncErrors: List<SyncError>): File = withContext(dispatchers.io) {
        logInfo("writeAll: ${syncErrors.size}")
        val dto = SyncErrorsDto(jsonGenerationDate = dateNow(),
            deviceInfo = buildSyncErrorDeviceInfoUseCase.build(), syncErrors = syncErrors.map { it.toDto() })
        val file = createLogFile()
        file.sink().buffer().use { sink ->
            syncErrorMetadataJsonMapper.writeToSink(dto, sink)
        }
        file
    }
}
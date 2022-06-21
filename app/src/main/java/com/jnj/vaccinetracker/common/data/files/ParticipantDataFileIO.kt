package com.jnj.vaccinetracker.common.data.files

import com.jnj.vaccinetracker.common.data.encryption.EncryptionService
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.exceptions.FileCollisionException
import com.jnj.vaccinetracker.common.helpers.*
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


class ParticipantDataFileIO @Inject constructor(
    private val androidFiles: AndroidFiles,
    private val encryptionService: EncryptionService,
    private val dispatchers: AppCoroutineDispatchers,
) {
    private val ParticipantDataFile.dateModified
        get() = when (this) {
            is DraftParticipantBiometricsTemplateFile -> null
            is ParticipantBiometricsTemplateFile -> dateModified
            is DraftParticipantImageFile -> null
            is ParticipantImageFile -> dateModified
        }

    private fun ParticipantDataFile.toFile(): File = toFile(androidFiles)

    fun exists(metadata: ParticipantDataFile): Boolean = metadata.toFile().exists()

    suspend fun deleteParticipantDataFile(metadata: ParticipantDataFile): Boolean = withContext(dispatchers.io) {
        logInfo("deleteParticipantDataFile: ${metadata.participantUuid} ${metadata::class.simpleName}")
        metadata.toFile().delete()
    }

    suspend fun writeParticipantDataFile(metadata: ParticipantDataFile, content: ByteArray, overwrite: Boolean, isEncrypted: Boolean = false): Unit = withContext(dispatchers.io) {
        val dst = metadata.toFile()
        if (!overwrite && dst.exists()) {
            throw FileCollisionException("$dst already exists -- for participant ${metadata.participantUuid}")
        }
        val tmp = dst.toTemp()
        tmp.delete()
        encryptionService.writeEncryptedFile(tmp, content, isAlreadyEncrypted = isEncrypted)
        metadata.dateModified?.let { tmp.setLastModified(it.time) }
        if (overwrite)
            dst.delete()
        if (tmp.renameTo(dst))
            dst
        else
            throw FileCollisionException("couldn't rename $tmp to $dst -- for participant ${metadata.participantUuid}")
    }

    suspend fun readParticipantDataFileContentOrThrow(participantDataFile: ParticipantDataFile): ByteArray {
        return readParticipantDataFileContent(participantDataFile) ?: throw IllegalArgumentException("no reading rights for file: $participantDataFile")
    }

    suspend fun readParticipantDataFileContent(participantDataFile: ParticipantDataFile): ByteArray? = withContext(dispatchers.io) {
        val src = participantDataFile.toFile()
        if (src.canRead())
            encryptionService.readEncryptedFile(src)
        else
            null
    }

    suspend fun deleteAllSyncImages(): Boolean = withContext(dispatchers.io) {
        logWarn("deleteAllSyncImages")
        ParticipantImageFile.toFile(androidFiles).deleteChildren()
    }

    suspend fun deleteAllSyncBiometricsTemplates(): Boolean = withContext(dispatchers.io) {
        logWarn("deleteAllSyncBiometricsTemplates")
        ParticipantBiometricsTemplateFile.toFile(androidFiles).deleteChildren()
    }
}
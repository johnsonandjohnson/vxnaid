package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.data.database.entities.base.SyncBase
import com.jnj.vaccinetracker.common.data.database.entities.base.UploadableDraft
import com.jnj.vaccinetracker.common.data.database.entities.base.UploadableDraftWithDate
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.helpers.readBytesOrNull
import com.jnj.vaccinetracker.common.helpers.uuid
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext


private const val BIOMETRICS_TEMPLATES_FOLDER = "biometrics_templates"
private const val IMAGE_FOLDER = "images"
private const val DRAFT_IMAGE_FOLDER = "draft_$IMAGE_FOLDER"
private const val DRAFT_BIOMETRICS_TEMPLATES_FOLDER = "draft_$BIOMETRICS_TEMPLATES_FOLDER"

private fun formatImageFileName(uuid: String) = "$uuid.jpg"
private fun formatBiometricsTemplateFileName(uuid: String) = "$uuid.dat"


sealed class ParticipantDataFile {
    abstract val participantUuid: String
    abstract val fileName: String
    abstract val isDraft: Boolean
}

@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class ParticipantImageFileBase : ParticipantDataFile() {

    override val isDraft: Boolean
        get() = this is DraftParticipantImageFile
}

@JsonClass(generateAdapter = true)
@TypeLabel("image")
data class ParticipantImageFile(
    override val participantUuid: String,
    override val fileName: String,
    override val dateModified: DateEntity,
) : ParticipantImageFileBase(), SyncBase {
    companion object : Folder {
        override val folderName = IMAGE_FOLDER
        fun newFile(participantUuid: String, dateModified: DateEntity = dateNow(), fileUuid: String = uuid()) =
            ParticipantImageFile(participantUuid, formatImageFileName(fileUuid), dateModified)
    }
}

@JsonClass(generateAdapter = true)
@TypeLabel("draft_image")
data class DraftParticipantImageFile(
    override val participantUuid: String,
    override val fileName: String,
    override val draftState: DraftState,
) : ParticipantImageFileBase(), UploadableDraft {
    companion object : Folder {
        override val folderName = DRAFT_IMAGE_FOLDER

        fun newFile(participantUuid: String, fileUuid: String = uuid()) =
            DraftParticipantImageFile(participantUuid, formatImageFileName(fileUuid), DraftState.initialState())
    }
}

@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class ParticipantBiometricsTemplateFileBase : ParticipantDataFile() {
    override val isDraft: Boolean
        get() = this is DraftParticipantBiometricsTemplateFile
}

@JsonClass(generateAdapter = true)
@TypeLabel("template")
data class ParticipantBiometricsTemplateFile(
    override val participantUuid: String,
    override val fileName: String,
    override val dateModified: DateEntity,
) : ParticipantBiometricsTemplateFileBase(), SyncBase {
    companion object : Folder {
        override val folderName = BIOMETRICS_TEMPLATES_FOLDER

        fun newFile(participantUuid: String, dateModified: DateEntity = dateNow(), fileUuid: String = uuid()) =
            ParticipantBiometricsTemplateFile(participantUuid, formatBiometricsTemplateFileName(fileUuid), dateModified)

    }
}

@JsonClass(generateAdapter = true)
@TypeLabel("draft_template")
data class DraftParticipantBiometricsTemplateFile(
    override val participantUuid: String,
    override val fileName: String,
    override val draftState: DraftState,
    override val dateLastUploadAttempt: DateEntity?,
) : ParticipantBiometricsTemplateFileBase(), UploadableDraftWithDate {

    companion object : Folder {
        override val folderName = DRAFT_BIOMETRICS_TEMPLATES_FOLDER

        fun newFile(participantUuid: String, fileUuid: String = uuid()) =
            DraftParticipantBiometricsTemplateFile(
                participantUuid = participantUuid,
                fileName = formatBiometricsTemplateFileName(fileUuid),
                draftState = DraftState.initialState(), dateLastUploadAttempt = null
            )
    }
}

interface Folder {
    val folderName: String
}

fun ParticipantDataFile.folder(): Folder = when (this) {
    is DraftParticipantBiometricsTemplateFile -> DraftParticipantBiometricsTemplateFile
    is ParticipantBiometricsTemplateFile -> ParticipantBiometricsTemplateFile
    is DraftParticipantImageFile -> DraftParticipantImageFile
    is ParticipantImageFile -> ParticipantImageFile
}


fun Folder.toFile(androidFiles: AndroidFiles): File = File(androidFiles.externalFiles, folderName)

fun ParticipantDataFile.toFile(androidFiles: AndroidFiles): File {
    val parentFile = folder().toFile(androidFiles)
    parentFile.mkdirs()
    return File(parentFile, fileName)
}

fun ParticipantDataFile.exists(androidFiles: AndroidFiles): Boolean = toFile(androidFiles).exists()
suspend fun ParticipantDataFile.readContent(androidFiles: AndroidFiles, context: CoroutineContext = Dispatchers.IO): ByteArray? = toFile(androidFiles).readBytesOrNull(context)

sealed class ParticipantDataFileWithContent {
    abstract val file: ParticipantDataFile
    abstract val content: String
}

@JsonClass(generateAdapter = true)
data class TemplateFileWithContent(override val file: ParticipantBiometricsTemplateFileBase, override val content: String) : ParticipantDataFileWithContent()

@JsonClass(generateAdapter = true)
data class ImageFileWithContent(override val file: ParticipantImageFileBase, override val content: String) : ParticipantDataFileWithContent()
package com.jnj.vaccinetracker.settings.domain.usecasse

import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBiometricsTemplateFile
import com.jnj.vaccinetracker.common.helpers.kb
import com.jnj.vaccinetracker.common.helpers.uuid
import javax.inject.Inject
import kotlin.random.Random

class GenerateTemplatesUseCase @Inject constructor(
    private val templateRepository: ParticipantBiometricsTemplateRepository,
    private val participantDataFileIO: ParticipantDataFileIO
) {
    private suspend fun generateTemplate(): ParticipantBiometricsTemplateFile {
        val file = ParticipantBiometricsTemplateFile.newFile(uuid())
        participantDataFileIO.writeParticipantDataFile(file, content = Random.nextBytes(5.kb.toInt()), overwrite = false, isEncrypted = true)
        return file
    }

    suspend fun generate() {
        templateRepository.insert(generateTemplate(), false)
    }
}
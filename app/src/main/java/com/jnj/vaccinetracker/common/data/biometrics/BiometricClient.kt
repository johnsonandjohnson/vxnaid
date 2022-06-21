package com.jnj.vaccinetracker.common.data.biometrics

import com.jnj.vaccinetracker.common.data.biometrics.models.BiometricMatch
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBiometricsTemplateFileBase
import com.jnj.vaccinetracker.common.domain.entities.toNBuffer
import com.jnj.vaccinetracker.common.exceptions.EnrollBiometricsTemplateFailed
import com.jnj.vaccinetracker.common.exceptions.IdentifyBiometricsTemplateFailed
import com.jnj.vaccinetracker.common.helpers.*
import com.neurotec.biometrics.NBiometricStatus
import com.neurotec.biometrics.NSubject
import com.neurotec.biometrics.client.NBiometricClient
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.Closeable


class BiometricClient constructor(
    private val biometricClient: NBiometricClient,
    private val dispatchers: AppCoroutineDispatchers,
    private val participantDataFileIO: ParticipantDataFileIO,
) : Closeable {

    suspend fun enrollSilently(biometricsTemplate: ParticipantBiometricsTemplateFileBase) {
        return try {
            enroll(biometricsTemplate)
        } catch (ex: Exception) {
            logError("failed to enroll: $biometricsTemplate", ex)
        }
    }


    private suspend fun ParticipantBiometricsTemplateFileBase.readSubject(): NSubject = withContext(dispatchers.io) {
        val biometricsTemplateBytes = readFile()
        val subject = NSubject()
        subject.templateBuffer = biometricsTemplateBytes.toNBuffer()
        subject.id = participantUuid
        subject
    }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun enroll(biometricsTemplate: ParticipantBiometricsTemplateFileBase) = withContext(dispatchers.io) {
        logInfo("enroll template ${biometricsTemplate.fileName} with participantUuid ${biometricsTemplate.participantUuid}")
        val status = try {
            val subject = biometricsTemplate.readSubject()
            biometricClient.enroll(subject, false)
        } catch (ex: Throwable) {
            throw EnrollBiometricsTemplateFailed("enroll failed error unknown (${biometricsTemplate.participantUuid})", ex)
        }
        if (status != NBiometricStatus.OK) {
            throw EnrollBiometricsTemplateFailed("enroll status not ok (${biometricsTemplate.participantUuid}): $status")
        }
    }

    suspend fun match(biometricsTemplateProbe: BiometricsTemplateBytes): List<BiometricMatch> = withContext(dispatchers.io) {
        logDebug("match")
        try {
            NSubject().use { nSubject ->
                nSubject.templateBuffer = biometricsTemplateProbe.toNBuffer()
                val status = biometricClient.identify(nSubject)
                logInfo("Match status : $status")
                if (status == NBiometricStatus.OK) {
                    nSubject.matchingResults.map {
                        BiometricMatch(it.id, it.score)
                    }.onEachIndexed { index, biometricMatch -> logInfo("match #$index = {}", biometricMatch) }
                } else {
                    if (nSubject.error != null) {
                        throw nSubject.error
                    }
                    logWarn("identify task failed: $status")
                    emptyList()
                }
            }
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            throw IdentifyBiometricsTemplateFailed(ex)
        }
    }

    private suspend fun ParticipantBiometricsTemplateFileBase.readFile(): BiometricsTemplateBytes {
        return BiometricsTemplateBytes(participantDataFileIO.readParticipantDataFileContentOrThrow(this))
    }

    override fun close() {
        biometricClient.close()
    }

}
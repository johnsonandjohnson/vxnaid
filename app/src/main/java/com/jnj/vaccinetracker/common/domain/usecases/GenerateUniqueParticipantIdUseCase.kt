package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.repositories.AutoGeneratedParticipantIdSequenceRepository
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.Site
import com.jnj.vaccinetracker.common.exceptions.DeviceNameNotAvailableException
import com.jnj.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject

class GenerateUniqueParticipantIdUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val getSelectedSiteUseCase: GetSelectedSiteUseCase,
    private val autoGeneratedParticipantIdSequenceRepository: AutoGeneratedParticipantIdSequenceRepository,
    private val findParticipantByParticipantIdUseCase: FindParticipantByParticipantIdUseCase,
) {
    companion object {
        private const val SEPARATOR = "-"
    }

    private fun createParticipantId(countryCode: String, deviceName: String, sequenceNumber: Int): String {
        val sequenceNumberString = sequenceNumber.toString().padStart(4, '0')
        return listOf(countryCode, deviceName, sequenceNumberString).joinToString(SEPARATOR)
    }

    private suspend fun doesParticipantIdExists(participantId: String): Boolean =
        findParticipantByParticipantIdUseCase.findByParticipantId(participantId) != null &&
        findParticipantByParticipantIdUseCase.findDeletedParticipantbyId(participantId) !=null

    private suspend fun generateUniqueParticipantId(site: Site, deviceName: String, sequenceNumber: Int): String {
        val countryCode = site.countryCode
        val participantIdCandidate = createParticipantId(countryCode = countryCode, deviceName = deviceName, sequenceNumber)
        return if (doesParticipantIdExists(participantIdCandidate)) {
            // current sequence is already in use so create the next one and store it
            val uniqueSequenceNumber = sequenceNumber + 1
            autoGeneratedParticipantIdSequenceRepository.storeSequence(deviceName, uniqueSequenceNumber)

            generateUniqueParticipantId(site, deviceName, uniqueSequenceNumber)
        } else {
            logInfo("generateUniqueParticipantId: $participantIdCandidate")
            participantIdCandidate
        }
    }

    suspend fun generateUniqueParticipantId(): String {
        val site = getSelectedSiteUseCase.getSelectedSite()
        val deviceName = userRepository.getDeviceName() ?: throw DeviceNameNotAvailableException()
        val participantSequenceNumber = autoGeneratedParticipantIdSequenceRepository.getSequence(deviceName)
            .coerceAtLeast(1)
        val participantId: String = generateUniqueParticipantId(site, deviceName, participantSequenceNumber)
        return participantId.also {
            logInfo("generateParticipantId: $it")
        }
    }
}
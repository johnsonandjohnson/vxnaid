package com.jnj.vaccinetracker.settings.domain.usecasse

import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.domain.entities.Address
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.domain.entities.Participant
import com.jnj.vaccinetracker.common.domain.usecases.GenerateUniqueParticipantIdUseCase
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.helpers.uuid
import kotlinx.coroutines.yield
import javax.inject.Inject

class GenerateFatParticipantUseCase @Inject constructor(
    private val participantRepository: ParticipantRepository,
    private val generateUniqueParticipantIdUseCase: GenerateUniqueParticipantIdUseCase
) {

    private fun generateRandomWord(n: Int): String {
        val charPool = 'a'..'z'
        return (1..n)
            .map { charPool.random() }
            .joinToString("")
    }

    private suspend fun createFatParticipant(): Participant {
        return Participant(
            uuid(), dateNow(), null, null,
            participantId = generateUniqueParticipantIdUseCase.generateUniqueParticipantId(),
            Gender.MALE, BirthDate.yearOfBirth(1994),
            address = Address(
                address1 = "Koekoekstraat",
                address2 = "40",
                cityVillage = "Beerse",
                stateProvince = null,
                country = "Belgium",
                countyDistrict = null,
                postalCode = "2340"
            ),
            attributes = mapOf("fake attribute" to generateRandomWord(100_000))

        )
    }

    suspend fun generate() {
        val participant = createFatParticipant()
        try {
            participantRepository.insert(participant, false)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("failed to insert new fat participant ${participant.participantId}", ex)
        }
    }
}
package com.jnj.vaccinetracker.fake.data.random

import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.api.response.AttributeDto
import com.jnj.vaccinetracker.common.data.models.toDto
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.fake.data.network.MockAssetReader
import com.jnj.vaccinetracker.fake.data.random.helper.RandomAddressGenerator
import com.jnj.vaccinetracker.fake.data.random.helper.RandomPhoneGenerator
import com.jnj.vaccinetracker.sync.data.models.ParticipantSyncRecord
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RandomParticipantGenerator @Inject constructor(
    private val randomPhoneGenerator: RandomPhoneGenerator,
    private val randomAddressGenerator: RandomAddressGenerator,
    private val mockAssetReader: MockAssetReader,
    private val dispatchers: AppCoroutineDispatchers,
    private val participantIdGenerator: FakeParticipantIdGenerator,
) {
    companion object {
        private const val MIN_YEAR = 1920
        private const val MAX_YEAR = 2020
    }

    private suspend fun randomAttributes(): List<AttributeDto> {
        val attributeList = mutableListOf<Pair<String, String>>()
        val phone = randomPhoneGenerator.generatePhone()
        if (phone != null) {
            attributeList += Constants.ATTRIBUTE_TELEPHONE to phone
        }
        val sites = mockAssetReader.readSites().results
        attributeList += listOf(
            Constants.ATTRIBUTE_OPERATOR to uuid(),
            Constants.ATTRIBUTE_VACCINE to "Covid 2D vaccine",
            Constants.ATTRIBUTE_LOCATION to sites.filter { it.country == "India" }.random().uuid,
            "person status" to "ACTIVATED",
            "personLanguage" to "English"
        )
        return attributeList
            .map { AttributeDto(it.first, it.second) }
    }

    suspend fun generateParticipant(dateModified: SyncDate): ParticipantSyncRecord.Update = withContext(dispatchers.io) {
        val yearRange = MIN_YEAR..MAX_YEAR
        ParticipantSyncRecord.Update(participantUuid = uuid(), participantId = participantIdGenerator.generateId(), dateModified = dateModified,
            gender = Gender.values().random(), birthDate = BirthDate.yearOfBirth(yearRange.random()).toDto(),
            address = randomAddressGenerator.generateAddress(),
            attributes = randomAttributes())
    }
}
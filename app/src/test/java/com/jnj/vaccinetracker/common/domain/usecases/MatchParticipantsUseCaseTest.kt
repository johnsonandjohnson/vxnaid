package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.models.ParticipantMatchDto
import com.jnj.vaccinetracker.common.data.models.toDomain
import com.jnj.vaccinetracker.common.data.models.toDto
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class MatchParticipantsUseCaseTest : FunSpec({
    val draftParticipantRepository: DraftParticipantRepository = mockk()
    val participantRepository: ParticipantRepository = mockk()
    val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository = mockk()
    val participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository = mockk()
    val biometricMatcherUseCase: BiometricsMatcherUseCase = mockk()
    val getSelectedSiteUseCase: GetSelectedSiteUseCase = mockk()
    val api: VaccineTrackerSyncApiDataSource = mockk()
    val config: Configuration = mockk()
    val getConfigurationUseCase: GetConfigurationUseCase = mockk()

    coEvery { getConfigurationUseCase.getMasterData() } returns config
    // sut => System Under Test
    val sut = MatchParticipantsUseCase(
        api,
        draftParticipantRepository,
        participantRepository,
        draftParticipantBiometricsTemplateRepository,
        participantBiometricsTemplateRepository,
        biometricMatcherUseCase,
        AppCoroutineDispatchers.DEFAULT,
        getSelectedSiteUseCase,
        getConfigurationUseCase
    )
    val selectedSite = Site(uuid = uuid(), name = "Wommelgem Clinic", country = "Belgium", cluster = "Antwerpen", siteCode = "WOM", countryCode = "BE")
    coEvery { getSelectedSiteUseCase.getSelectedSite() } returns selectedSite

    fun identificationCriteria(participantId: String? = null, phone: String? = null, biometricsTemplate: BiometricsTemplateBytes? = null) = ParticipantIdentificationCriteria(
        participantId = participantId,
        phone = phone,
        biometricsTemplate = biometricsTemplate)

    fun ParticipantBase.toMatch(matchingScore: Int? = null) =
        ParticipantMatch(participantUuid, participantId, matchingScore, gender, birthDate, address, attributes)

    fun ParticipantBiometricsTemplateFileBase.toMatch(matchingScore: Int) = BiometricsFileMatch(this, matchingScore)
    fun participantMatch(uuid: String = uuid(), participantId: String, matchingScore: Int? = null): ParticipantMatch {
        return ParticipantMatch(uuid, participantId, matchingScore, BirthDate.yearOfBirth(2000), null, emptyMap())
    }

    fun participantMatchDto(uuid: String = uuid(), participantId: String, matchingScore: Int? = null): ParticipantMatchDto {
        return ParticipantMatchDto(uuid, participantId, matchingScore, BirthDate.yearOfBirth(2000).toDto(), null, emptyList())
    }

    fun participant(uuid: String = uuid(), participantId: String = "", nin: String = "", phone: String? = null, gender: Gender = Gender.MALE, withTemplate: Boolean = true) =
        Participant(uuid, DateEntity(), null, if (withTemplate) ParticipantBiometricsTemplateFile.newFile(uuid) else null, participantId, nin, gender, BirthDate.yearOfBirth(2000),
            mapOf<String,String>() .withBirthWeight(birthWeight) null)
            mapOf<String, String>().withPhone(phone), null)

    fun draftParticipant(uuid: String = uuid(), participantId: String = "", nin: String = "", phone: String? = null, gender: Gender = Gender.FEMALE, withTemplate: Boolean = true) =
        DraftParticipant(
            uuid,
            DateEntity(),
            null,
            if (withTemplate) DraftParticipantBiometricsTemplateFile.newFile(uuid) else null,
            participantId,
            nin,
            gender,
            BirthDate.yearOfBirth(2000),
            mapOf<String, String>().withPhone(phone),
            mapOf<String, String>().withBirthWeight(birthWeight)
            null,
            DraftState.initialState(),
            birthWeight
        )
    test("when remote api successfully returns result then return that") {
        // Arrange
        val participantId = "1"
        val match = participantMatchDto("11115", participantId = participantId)
        coEvery { api.matchParticipants(participantId, any(), any(), selectedSite.country) } returns listOf(match)
        coEvery { participantRepository.findByParticipantId(any()) } throws Exception("local match err")
        // Act
        val result = sut.matchParticipants(identificationCriteria(participantId = participantId))
        // Assert
        result shouldBe listOf(match.toDomain())
    }

    context("when remote api fails") {
        coEvery { api.matchParticipants(any(), any(), any(), selectedSite.country) } throws NoNetworkException()
        test("then use local matching instead of remote") {
            // Arrange
            val participantId = "1"
            val participant = participant(participantId = participantId)
            coEvery { participantRepository.findByParticipantId(participantId) } returns participant
            // Act
            val result = sut.matchParticipants(identificationCriteria(participantId))
            // Assert
            result shouldBe listOf(participant.toMatch())
        }
        context("and only participant id is specified") {
            val participantId = "235"
            suspend fun doTest(participant: ParticipantBase) {
                // Act
                val result = sut.matchParticipants(identificationCriteria(participant.participantId))
                // Assert
                result shouldBe listOf(participant.toMatch())
            }
            test("and draft_participant table contains the participantId but participant table does not then return draft_participant") {
                // Arrange
                val draftParticipant = draftParticipant(participantId = participantId)
                coEvery { participantRepository.findByParticipantId(draftParticipant.participantId) } returns null
                coEvery { draftParticipantRepository.findByParticipantId(draftParticipant.participantId) } returns draftParticipant
                doTest(draftParticipant)
            }
            test("and participant table contains the participantId but draft_participant table does not then return participant") {
                // Arrange
                val participant = participant(participantId = participantId)
                coEvery { participantRepository.findByParticipantId(participant.participantId) } returns participant

                doTest(participant)

            }
            test("and participant table, draft_participant table both contain the participant id then return the former table row (participant_table)") {
                // Arrange
                val participant = participant(participantId = participantId)
                coEvery { participantRepository.findByParticipantId(participant.participantId) } returns participant
                val draftParticipant = draftParticipant(participantId = participantId)
                coEvery { draftParticipantRepository.findByParticipantId(draftParticipant.participantId) } returns draftParticipant
                doTest(participant)
            }
        }
        context("and biometricsTemplate is specified") {
            val biometricsTemplateBytes = BiometricsTemplateBytes(ByteArray(1))
            context("and phone is specified as well") {
                val phone = "0123 56 6"
                suspend fun doTest(participants: List<Participant>, draftParticipants: List<DraftParticipant>) {
                    // Arrange
                    val matchingScore = 1

                    val uniqueParticipants = (participants + draftParticipants).distinctBy { it.participantUuid }
                    val uniqueTemplates = uniqueParticipants.mapNotNull { it.biometricsTemplate }
                    coEvery { participantRepository.findAllByPhone(phone) } returns participants
                    coEvery { draftParticipantRepository.findAllByPhone(phone) } returns draftParticipants
                    coEvery { draftParticipantRepository.findByParticipantUuid(any()) } coAnswers { inv ->
                        val id = inv.invocation.args[0]
                        draftParticipants.find { it.participantUuid == id }
                    }
                    coEvery { participantRepository.findByParticipantUuid(any()) } coAnswers { inv ->
                        val id = inv.invocation.args[0]
                        participants.find { it.participantUuid == id }
                    }
                    coEvery { biometricMatcherUseCase.match(uniqueTemplates, biometricsTemplateBytes) } returns uniqueTemplates.map { it.toMatch(matchingScore) }

                    val matches = uniqueParticipants.map { it.toMatch(matchingScore) }
                    print(uniqueTemplates)
                    print(biometricsTemplateBytes)
                    // Act
                    val result = sut.matchParticipants(identificationCriteria(phone = phone, biometricsTemplate = biometricsTemplateBytes))
                    // Assert
                    result shouldBe matches
                }
                test("and draft_participant table contains the phone but participant table does not then return draft_participant") {
                    // Arrange
                    val draftParticipants = generateSequence { draftParticipant(phone = phone) }.take(3).toList()

                    doTest(participants = emptyList(), draftParticipants = draftParticipants)
                }
                test("and participant table contains the phone but draft_participant table does not then return participant") {
                    // Arrange
                    val participants = generateSequence { participant(phone = phone) }.take(3).toList()
                    doTest(participants = participants, draftParticipants = emptyList())
                }
                test("and participant table, draft_participant table both contain the phone then return results from both tables") {
                    // Arrange
                    val participants = generateSequence { participant(uuid = "1", phone = phone) }.take(3).toList()
                    val draftParticipants = generateSequence { draftParticipant(uuid = "2", phone = phone) }.take(3).toList()
                    doTest(participants = participants, draftParticipants = draftParticipants)
                }
            }
            context("and participantId is specified as well") {
                val participantId = "012"
                suspend fun doTest(participant: Participant?, draftParticipant: DraftParticipant?) {
                    // Arrange
                    val matchingScore = 1
                    val uniqueParticipants = listOfNotNull(participant ?: draftParticipant)
                    val uniqueTemplates = uniqueParticipants.mapNotNull { it.biometricsTemplate }
                    coEvery { participantRepository.findByParticipantId(participantId) } returns participant
                    coEvery { draftParticipantRepository.findByParticipantId(participantId) } returns draftParticipant
                    coEvery { draftParticipantRepository.findByParticipantUuid(any()) } coAnswers { inv ->
                        val id = inv.invocation.args[0]
                        draftParticipant?.takeIf { it.participantUuid == id }
                    }
                    coEvery { participantRepository.findByParticipantUuid(any()) } coAnswers { inv ->
                        val id = inv.invocation.args[0]
                        participant?.takeIf { it.participantUuid == id }
                    }
                    val matches = uniqueParticipants.map { it.toMatch(matchingScore) }
                    coEvery { biometricMatcherUseCase.match(uniqueTemplates, biometricsTemplateBytes) } returns uniqueTemplates.map { it.toMatch(matchingScore) }
                    // Act
                    val result = sut.matchParticipants(identificationCriteria(participantId = participantId, biometricsTemplate = biometricsTemplateBytes))
                    // Assert
                    result shouldBe matches
                }
                test("and draft_participant table contains the participantId but participant table does not then return draft_participant") {
                    doTest(participant = null, draftParticipant = draftParticipant(participantId = participantId))
                }
                test("and participant table contains the participantId but draft_participant table does not then return participant") {
                    doTest(
                        participant = participant(participantId = participantId),
                        draftParticipant = null,
                    )
                }
                test("and participant table, draft_participant table both contain the participantId then return the first participant one") {
                    doTest(
                        participant = participant(participantId = participantId),
                        draftParticipant = draftParticipant(participantId = participantId),
                    )
                }
            }
            context("and both participantId and phone are specified") {
                test("then return matches for biometric+participantId and matches for biometric+phone") {
                    // Arrange
                    val participantId = "1234"
                    val phone = "323424324"
                    val participantWithPhone = participant(phone = phone)
                    val participantWithParticipantId = participant(participantId = participantId)
                    val uniqueParticipants = listOf(participantWithParticipantId, participantWithPhone)
                    val matchingScore = 1
                    coEvery { participantRepository.findAllByPhone(phone) } returns listOfNotNull(participantWithPhone)
                    coEvery { draftParticipantRepository.findAllByPhone(phone) } returns emptyList()
                    coEvery { participantRepository.findByParticipantId(participantId) } returns participantWithParticipantId
                    coEvery { participantRepository.findByParticipantUuid(any()) } coAnswers { inv ->
                        val id = inv.invocation.args[0]
                        uniqueParticipants.find { it.participantUuid == id }
                    }
                    val templates = listOfNotNull(participantWithParticipantId.biometricsTemplate, participantWithPhone.biometricsTemplate)
                    val matches = uniqueParticipants.map { it.toMatch(matchingScore) }
                    coEvery {
                        biometricMatcherUseCase.match(templates,
                            biometricsTemplateBytes)
                    } returns templates.map { it.toMatch(matchingScore) }
                    // Act
                    val result = sut.matchParticipants(identificationCriteria(participantId = participantId,
                        phone = phone,
                        biometricsTemplateBytes))
                    // Assert
                    result shouldBe matches
                }
            }

            context("and neither participantId nor phone are specified") {
                suspend fun doTest() {
                    // Arrange
                    val participantId: String? = null
                    val phone: String? = null
                    val participantWithoutPhone = participant(phone = phone)
                    val draftParticipantWithoutPhone = draftParticipant(phone = phone)
                    val uniqueParticipants = listOf(participantWithoutPhone, draftParticipantWithoutPhone)
                    val matchingScore = 1
                    coEvery { draftParticipantBiometricsTemplateRepository.findAll() } returns listOfNotNull(draftParticipantWithoutPhone.biometricsTemplate)
                    coEvery { participantBiometricsTemplateRepository.findAll() } returns listOfNotNull(participantWithoutPhone.biometricsTemplate)
                    coEvery { participantRepository.findByParticipantUuid(any()) } coAnswers { _ ->
                        participantWithoutPhone
                    }
                    coEvery { draftParticipantRepository.findByParticipantUuid(any()) } coAnswers { _ ->
                        draftParticipantWithoutPhone
                    }
                    val templates = listOfNotNull(participantWithoutPhone.biometricsTemplate, draftParticipantWithoutPhone.biometricsTemplate)
                    val matches = uniqueParticipants.map { it.toMatch(matchingScore) }
                    matches.size shouldBe 2
                    coEvery {
                        biometricMatcherUseCase.match(templates,
                            biometricsTemplateBytes)
                    } returns templates.map { it.toMatch(matchingScore) }
                    // Act
                    val result = sut.matchParticipants(identificationCriteria(participantId = participantId,
                        phone = phone,
                        biometricsTemplateBytes))
                    // Assert
                    result shouldBe matches
                }
                test("then return matches for all templates") {
                    doTest()
                }
            }
        }
    }
})
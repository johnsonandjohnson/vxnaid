package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.api.response.ObservationDto
import com.jnj.vaccinetracker.common.data.models.api.response.VisitDetailDto
import com.jnj.vaccinetracker.common.data.models.api.response.toDomain
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.days
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.TestCoroutineDispatcher

class GetParticipantVisitDetailsUseCaseTest : FunSpec({
    val participantUuid = uuid()
    val visits = mutableListOf<Visit>()
    val draftVisits = mutableListOf<DraftVisit>()
    val draftVisitEncounters = mutableListOf<DraftVisitEncounter>()
    val visitDetails = mutableListOf<VisitDetailDto>()
    val draftVisitRepository: DraftVisitRepository = createDraftVisitRepository(draftVisits)
    val draftVisitEncounterRepository: DraftVisitEncounterRepository = createDraftVisitEncounterRepository(draftVisitEncounters)
    val visitRepository: VisitRepository = createVisitRepository(visits)
    val api: VaccineTrackerSyncApiDataSource = createApi(visitDetails)
    val dispatcher = TestCoroutineDispatcher()
    val dispatchers = AppCoroutineDispatchers.fromSingleDispatcher(dispatcher)
    val useCase = GetParticipantVisitDetailsUseCase(draftVisitRepository, draftVisitEncounterRepository, visitRepository, api, dispatchers)

    fun date(days: Int): DateEntity = DateEntity(days.days)

    fun createVisit(visitUuid: String, startDateTime: DateEntity, encounterDate: DateEntity? = null) =
        Visit(
            startDatetime = startDateTime,
            participantUuid = participantUuid,
            visitUuid = visitUuid,
            attributes = emptyMap(),
            observations = if (encounterDate != null) mapOf(Constants.OBSERVATION_TYPE_MANUFACTURER to ObservationValue("Moderna", encounterDate)) else emptyMap(),
            dateModified = dateNow(),
            visitType = if (encounterDate == null) Constants.VISIT_STATUS_SCHEDULED else Constants.VISIT_STATUS_OCCURRED
        )

    fun createVisitDetail(visitUuid: String, startDateTime: DateEntity, encounterDate: DateEntity? = null) =
        VisitDetailDto(
            visitDate = startDateTime,
            uuid = visitUuid,
            attributes = emptyList(),
            observations = if (encounterDate != null) listOf(ObservationDto(Constants.OBSERVATION_TYPE_MANUFACTURER, "Moderna", encounterDate)) else emptyList(),
            visitType = if (encounterDate == null) Constants.VISIT_STATUS_SCHEDULED else Constants.VISIT_STATUS_OCCURRED
        )


    fun createDraftVisit(visitUuid: String, startDateTime: DateEntity, visitType: String = Constants.VISIT_STATUS_SCHEDULED, draftState: DraftState = DraftState.UPLOADED) =
        DraftVisit(
            startDateTime,
            participantUuid,
            locationUuid = uuid(),
            visitUuid,
            attributes = emptyMap(),
            visitType = visitType,
            draftState = draftState,
        )


    fun createDraftEncounter(visitUuid: String, startDateTime: DateEntity, draftState: DraftState = DraftState.UPLOADED) =
        DraftVisitEncounter(
            startDatetime = startDateTime,
            participantUuid = participantUuid,
            locationUuid = uuid(),
            visitUuid = visitUuid,
            attributes = emptyMap(),
            observations = mapOf(Constants.OBSERVATION_TYPE_MANUFACTURER to "Moderna"),
            draftState = draftState,
        )

    suspend fun doUseCaseTest(startDateTime: Int, encounterDate: Int?) {
        // Act
        val results = useCase.getParticipantVisitDetails(participantUuid)
        // Assert
        results shouldBe listOf(createVisitDetail("1", date(startDateTime), encounterDate = encounterDate?.let { date(it) }).toDomain())
    }

    suspend fun doDraftVisitTests(startDateTime: Int, encounterDate: Int) {
        draftVisits += createDraftVisit("1", date(4))
        //draft visit available
        doUseCaseTest(startDateTime, encounterDate)
    }


    test("given empty results both local and remote then return empty list") {
        // Arrange
        // Act
        val results = useCase.getParticipantVisitDetails(participantUuid)
        // Assert
        results.shouldBeEmpty()
    }

    test("given empty api results and empty sync records then return draft results") {
        // Arrange
        draftVisits += createDraftVisit("1", date(1))
        // Act
        val results = useCase.getParticipantVisitDetails(participantUuid)
        // Assert
        results shouldBe listOf(createVisitDetail("1", date(1)).toDomain())
    }

    context("given remote visits are outdated") {
        visitDetails += createVisitDetail("1", date(0), encounterDate = date(0))
        context("and draft visit encounter available") {
            draftVisitEncounters += createDraftEncounter("1", date(5))

            context("and sync visit older than draft visit encounter") {
                visits += createVisit("1", date(2), encounterDate = date(3))
                test("then return sync visit with observations of sync visit") {
                    doDraftVisitTests(2, 3)
                }
            }
            test("and sync visit newer than draft visit encounter then return sync visit with observations of sync visit") {
                visits += createVisit("1", date(2), encounterDate = date(7))
                doDraftVisitTests(2, 7)
            }
            context("and sync visit not available") {
                visits.shouldBeEmpty()
                test("and draft visit available then return draft visit with observations of draft visit encounter") {
                    draftVisits += createDraftVisit("1", visitType = Constants.VISIT_STATUS_OCCURRED, startDateTime = date(4))
                    doUseCaseTest(4, 5)
                }
            }
        }

        context("and draft visit encounter not available") {

            test("and sync visit most recent than remote visit then return sync visit") {
                visits += createVisit("1", date(2), encounterDate = date(10))
                draftVisits.shouldBeEmpty()
                doUseCaseTest(2, 10)
            }
        }
    }


    context("given remote visits are newer than sync visits and draft visit encounters") {
        visitDetails += createVisitDetail("1", date(0), encounterDate = date(10))
        context("and draft visit encounter available") {
            draftVisitEncounters += createDraftEncounter("1", date(5))

            test("and sync visit older than draft visit encounter then return remote visit with observations of remote visit") {
                visits += createVisit("1", date(2), encounterDate = date(3))
                doDraftVisitTests(0, 10)
            }
            context("and sync visit newer than draft visit encounter then return remote visit with observations of remote visit") {
                visits += createVisit("1", date(2), encounterDate = date(7))
                doDraftVisitTests(0, 10)
            }
            context("and sync visit not available then return remote visit with observations of remote visit") {
                visits.shouldBeEmpty()
                doDraftVisitTests(0, 10)
            }
        }
        context("and draft visit encounter not available then return remote visit") {
            draftVisits += createDraftVisit("1", date(4))
            visits += createVisit("1", date(2), encounterDate = date(3))
            doUseCaseTest(0, 10)
        }
    }

})

private fun createApi(visits: List<VisitDetailDto>): VaccineTrackerSyncApiDataSource {
    val api: VaccineTrackerSyncApiDataSource = mockk()
    coEvery { api.getParticipantVisitDetails(any()) } coAnswers {
        visits
    }
    return api
}

private fun createVisitRepository(visits: List<Visit>): VisitRepository {
    val repo: VisitRepository = mockk()
    coEvery { repo.findAllByParticipantUuid(any()) } coAnswers {
        val participantUuid = invocation.args.first() as String
        visits.filter { it.participantUuid == participantUuid }
    }
    coEvery { repo.findByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.find { it.visitUuid == visitUuid }
    }
    return repo
}

private fun createDraftVisitEncounterRepository(visits: List<DraftVisitEncounter>): DraftVisitEncounterRepository {
    val repo: DraftVisitEncounterRepository = mockk()
    coEvery { repo.findAllByParticipantUuid(any()) } coAnswers {
        val participantUuid = invocation.args.first() as String
        visits.filter { it.participantUuid == participantUuid }
    }
    coEvery { repo.findByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.find { it.visitUuid == visitUuid }
    }
    return repo
}

private fun createDraftVisitRepository(visits: List<DraftVisit>): DraftVisitRepository {
    val repo: DraftVisitRepository = mockk()
    coEvery { repo.findAllByParticipantUuid(any()) } coAnswers {
        val participantUuid = invocation.args.first() as String
        visits.filter { it.participantUuid == participantUuid }
    }
    coEvery { repo.findByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.find { it.visitUuid == visitUuid }
    }
    return repo
}
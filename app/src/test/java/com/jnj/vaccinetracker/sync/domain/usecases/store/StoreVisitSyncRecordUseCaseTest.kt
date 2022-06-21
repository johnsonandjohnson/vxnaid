package com.jnj.vaccinetracker.sync.domain.usecases.store

import FakeTransactionRunner
import com.jnj.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.api.response.ObservationDto
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.DraftVisit
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.helpers.days
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.data.models.VisitSyncRecord
import com.jnj.vaccinetracker.sync.data.models.VisitSyncRecord.Delete.Companion.toDomain
import com.jnj.vaccinetracker.sync.domain.entities.DeletedSyncRecord
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.mockk.coEvery
import io.mockk.mockk
import java.util.*


class StoreVisitSyncRecordUseCaseTest : FunSpec({

    val draftVisits = mutableListOf<DraftVisit>()
    val draftVisitEncounters = mutableListOf<DraftVisitEncounter>()
    val visits = mutableListOf<Visit>()
    val deletedVisits = mutableListOf<DeletedSyncRecord>()


    val visitRepository: VisitRepository = createVisitRepository(visits)
    val draftVisitEncounterRepository: DraftVisitEncounterRepository = createDraftVisitEncounterRepository(draftVisitEncounters)
    val draftVisitRepository: DraftVisitRepository = createDraftVisitRepository(draftVisits)
    val transactionRunner: ParticipantDbTransactionRunner = FakeTransactionRunner()
    val deletedSyncRecordRepository: DeletedSyncRecordRepository = createDeletedSyncRecordsRepository(deletedVisits)
    val syncLogger: SyncLogger = mockk()


    val storeVisitSyncRecordUseCase = StoreVisitSyncRecordUseCase(
        visitRepository,
        draftVisitEncounterRepository,
        draftVisitRepository,
        transactionRunner,
        deletedSyncRecordRepository,
        syncLogger
    )

    fun date(days: Int): DateEntity = DateEntity(days.days)

    fun createDraftVisit(
        visitUuid: String,
        participantUuid: String,
        startDateTime: DateEntity,
        visitType: String = Constants.VISIT_STATUS_SCHEDULED,
        draftState: DraftState = DraftState.UPLOAD_PENDING,
    ) =
        DraftVisit(
            startDateTime,
            participantUuid,
            locationUuid = uuid(),
            visitUuid,
            attributes = emptyMap(),
            visitType = visitType,
            draftState,
        )

    fun createDraftEncounter(visitUuid: String, participantUuid: String, startDateTime: DateEntity, draftState: DraftState = DraftState.UPLOAD_PENDING) =
        DraftVisitEncounter(
            startDatetime = startDateTime,
            participantUuid = participantUuid,
            locationUuid = uuid(),
            visitUuid = visitUuid,
            attributes = emptyMap(),
            observations = mapOf(Constants.OBSERVATION_TYPE_MANUFACTURER to "Moderna"),
            draftState,
        )


    context("Store tests") {

        coEvery { syncLogger.clearSyncError(any()) } returns Unit

        context("Update") {

            val visit = VisitSyncRecord.Update(
                participantUuid = "001",
                dateModified = SyncDate(Date()),
                visitUuid = "1",
                visitType = Constants.VISIT_TYPE_DOSING,
                startDatetime = date(2),
                attributes = listOf(),
                observations = listOf(ObservationDto("name", "Moderna", date(2)))
            )

            context("Draft encounter") {
                test("No draft encounter present") {
                    // draftVisits is empty
                    storeVisitSyncRecordUseCase.store(visit)
                    //drafts should remain empty
                    draftVisitEncounters.shouldBeEmpty()
                }

                test("draft encounter present, no match - other visit UUID") {
                    draftVisitEncounters += createDraftEncounter("2", "001", date(2))
                    storeVisitSyncRecordUseCase.store(visit)
                    //draft should NOT be removed
                    draftVisitEncounters.shouldNotBeEmpty()
                }
                test("draft encounter present, no match - other participant UUID") {
                    draftVisitEncounters += createDraftEncounter("1", "002", date(2))
                    storeVisitSyncRecordUseCase.store(visit)
                    //draft should NOT be removed
                    draftVisitEncounters.shouldNotBeEmpty()
                }
                test("draft encounter present, with match") {
                    draftVisitEncounters += createDraftEncounter("1", "001", date(2))
                    storeVisitSyncRecordUseCase.store(visit)
                    //draft should be removed
                    draftVisitEncounters.shouldBeEmpty()

                }
            }
            context("Draft visit") {
                test("No draft visit present") {
                    // draftVisits is empty
                    storeVisitSyncRecordUseCase.store(visit)
                    //draft should remain empty
                    draftVisits.shouldBeEmpty()
                }

                test("draft visit present, no match - Other visit Uuid") {
                    draftVisits += createDraftVisit("2", "001", date(2))
                    storeVisitSyncRecordUseCase.store(visit)
                    //draft should be removed
                    draftVisits.shouldNotBeEmpty()
                }
                test("draft visit present, no match - Other participant Uuid") {
                    draftVisits += createDraftVisit("1", "002", date(2))
                    storeVisitSyncRecordUseCase.store(visit)
                    //draft should be removed
                    draftVisits.shouldNotBeEmpty()
                }
                test("draft visit present, with match") {
                    draftVisits += createDraftVisit("1", "001", date(2))
                    storeVisitSyncRecordUseCase.store(visit)
                    //draft should be removed
                    draftVisits.shouldBeEmpty()
                }
            }
        }

        context("Delete") {
            val deletedVisit = VisitSyncRecord.Delete(
                participantUuid = "001",
                dateModified = SyncDate(date(2)),
                visitUuid = "1"
            )
            val localVisit = Visit(
                participantUuid = "001",
                dateModified = date(2),
                visitUuid = "1",
                visitType = Constants.VISIT_TYPE_DOSING,
                startDatetime = date(2),
                attributes = emptyMap(),
                observations = emptyMap()
            )
            test("should delete visit and add it to the deleted syncrecords") {
                visits += localVisit
                storeVisitSyncRecordUseCase.store(deletedVisit)
                //make sure draft visit is deleted
                visits.shouldNotContain(localVisit)
                //make sure deleted visit is added to deleted visits list
                deletedVisits.shouldContain(deletedVisit.toDomain())
            }

        }
    }

})


private fun createDraftVisitRepository(visits: MutableList<DraftVisit>): DraftVisitRepository {
    val repo: DraftVisitRepository = mockk()
    coEvery { repo.findAllByParticipantUuid(any()) } coAnswers {
        val participantUuid = invocation.args.first() as String
        visits.filter { it.participantUuid == participantUuid }
    }
    coEvery { repo.findDraftStateByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.find { it.visitUuid == visitUuid }?.draftState
    }
    coEvery { repo.findByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.find { it.visitUuid == visitUuid }
    }
    coEvery { repo.deleteByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.remove(visits.find { it.visitUuid == visitUuid })
    }
    coEvery { repo.deleteByVisitUuid(any(), any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.remove(visits.find { it.visitUuid == visitUuid })
    }

    return repo
}

private fun createVisitRepository(visits: MutableList<Visit>): VisitRepository {
    val repo: VisitRepository = mockk()
    coEvery { repo.insert(any(), orReplace = true) } coAnswers {
        val visit = invocation.args.first() as Visit
        visits.add(visit)
    }
    coEvery { repo.deleteByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.remove(visits.find { it.visitUuid == visitUuid })
    }
    return repo
}

private fun createDraftVisitEncounterRepository(visits: MutableList<DraftVisitEncounter>): DraftVisitEncounterRepository {
    val repo: DraftVisitEncounterRepository = mockk()
    coEvery { repo.findAllByParticipantUuid(any()) } coAnswers {
        val participantUuid = invocation.args.first() as String
        visits.filter { it.participantUuid == participantUuid }
    }
    coEvery { repo.findByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.find { it.visitUuid == visitUuid }
    }
    coEvery { repo.findDraftStateByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.find { it.visitUuid == visitUuid }?.draftState
    }
    coEvery { repo.deleteByVisitUuid(any(), any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.remove(visits.find { it.visitUuid == visitUuid })
    }
    coEvery { repo.deleteByVisitUuid(any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.remove(visits.find { it.visitUuid == visitUuid })
    }
    coEvery { repo.deleteByVisitUuid(any(), any()) } coAnswers {
        val visitUuid = invocation.args.first() as String
        visits.remove(visits.find { it.visitUuid == visitUuid })
    }


    return repo
}

private fun createDeletedSyncRecordsRepository(visits: MutableList<DeletedSyncRecord>): DeletedSyncRecordRepository {
    val repo: DeletedSyncRecordRepository = mockk()
    coEvery { repo.insert(any(), orReplace = true) } coAnswers {
        val visit = invocation.args.first() as DeletedSyncRecord
        visits.add(visit)
    }
    return repo
}



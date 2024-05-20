package com.jnj.vaccinetracker.common.domain.usecases

import FakeTransactionRunner
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.exceptions.ParticipantAlreadyExistsException
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.upload.UploadDraftParticipantUseCase
import io.kotest.assertions.fail
import io.kotest.assertions.shouldFail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.*
import io.mockk.verify

class RegisterParticipantUseCaseTest : FunSpec({

    val draftParticipants = mutableListOf<DraftParticipant>()


    val draftParticipantRepository: DraftParticipantRepository = createDraftParticipantRepository(draftParticipants)
    val uploadDraftParticipantUseCase: UploadDraftParticipantUseCase = mockk()
    val createVisitUseCase: CreateVisitUseCase = mockk()
    val participantDataFileIO: ParticipantDataFileIO = mockk()
    val findParticipantByParticipantIdUseCase: FindParticipantByParticipantIdUseCase = mockk()
    val transactionRunner: ParticipantDbTransactionRunner = FakeTransactionRunner()
    val syncLogger: SyncLogger = mockk()
    val registerParticipantUseCase = RegisterParticipantUseCase(
        participantDataFileIO,
        draftParticipantRepository,
        uploadDraftParticipantUseCase,
        createVisitUseCase,
        findParticipantByParticipantIdUseCase,
        transactionRunner,
        syncLogger
    )

    val participantUuid = uuid()
    val date = dateNow()


    fun createScheduleFirstVisit(): ScheduleFirstVisit {
        return ScheduleFirstVisit(
            visitType = Constants.VISIT_TYPE_DOSING,
            startDatetime = date,
            locationUuid = uuid(),
            attributes = mapOf(
                Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                Constants.ATTRIBUTE_OPERATOR to uuid(),
                Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to "1"
            )
        )

    }

    val registerParticipant = RegisterParticipant(
        image = ImageBytes(ByteArray(1)),
        biometricsTemplate = BiometricsTemplateBytes(ByteArray(1)),
        participantId = participantUuid,
        nin = participantUuid,
        gender = Gender.MALE,
        birthdate = BirthDate.yearOfBirth(1994),
        address = Address(
            address1 = "Koekoekstraat",
            address2 = "40",
            cityVillage = "Beerse",
            stateProvince = null,
            country = "Belgium",
            countyDistrict = null,
            postalCode = "2340"
        ),
        scheduleFirstVisit = createScheduleFirstVisit(),
        attributes = emptyMap(),
    )

    val uploadedDraftParticipantTemplatesPending = DraftParticipant(
        participantUuid = participantUuid,
        registrationDate = date,
        image = DraftParticipantImageFile(participantUuid, "$participantUuid.jpg", DraftState.UPLOAD_PENDING),
        biometricsTemplate = DraftParticipantBiometricsTemplateFile(participantUuid, "$participantUuid.dat", DraftState.UPLOAD_PENDING, dateNow()),
        participantId = registerParticipant.participantId,
        nin = "NIN$registerParticipant.participantId",
        gender = registerParticipant.gender,
        birthDate = registerParticipant.birthdate,
        attributes = registerParticipant.attributes,
        address = registerParticipant.address,
        draftState = DraftState.UPLOADED
    )

    val uploadedDraftParticipantTemplatesUploaded = DraftParticipant(
        participantUuid = participantUuid,
        registrationDate = dateNow(),
        image = DraftParticipantImageFile(participantUuid, "$participantUuid.jpg", DraftState.UPLOADED),
        biometricsTemplate = DraftParticipantBiometricsTemplateFile(participantUuid, "$participantUuid.dat", DraftState.UPLOADED, dateNow()),
        participantId = registerParticipant.participantId,
        nin = "NIN$registerParticipant.participantId",
        gender = registerParticipant.gender,
        birthDate = registerParticipant.birthdate,
        attributes = registerParticipant.attributes,
        address = registerParticipant.address,
        draftState = DraftState.UPLOADED
    )

    val draftVisit = DraftVisit(
        startDatetime = date,
        participantUuid = participantUuid,
        locationUuid = uuid(),
        visitType = Constants.VISIT_TYPE_DOSING,
        draftState = DraftState.UPLOAD_PENDING,
        attributes = emptyMap(),
        visitUuid = uuid()
    )

    context("Register participant") {
        coEvery { participantDataFileIO.writeParticipantDataFile(any(), any(), any()) } returns Unit
        coEvery { createVisitUseCase.createVisit(any()) } returns draftVisit
        coEvery { syncLogger.logSyncError(any(), any()) } returns Unit

        test("templates not uploaded") {
            coEvery { findParticipantByParticipantIdUseCase.findByParticipantId(participantUuid) } returns null
            coEvery { uploadDraftParticipantUseCase.upload(any(), allowDuplicate = false, updateDraftState = false) } returns uploadedDraftParticipantTemplatesPending
            require(draftParticipants.size == 0)

            registerParticipantUseCase.registerParticipant(registerParticipant)
            //verify if sync error has been logged
            verify(exactly = 1) { syncLogger.logSyncError(any(), any()) }
            draftParticipants.size.shouldBe(1)
            draftParticipants[0].draftState.shouldBe(DraftState.UPLOADED)
            draftParticipants[0].biometricsTemplate?.draftState.shouldBe(DraftState.UPLOAD_PENDING)
        }
        test("participant successfully uploaded") {
            coEvery { findParticipantByParticipantIdUseCase.findByParticipantId(participantUuid) } returns null
            coEvery { uploadDraftParticipantUseCase.upload(any(), allowDuplicate = false, updateDraftState = false) } returns uploadedDraftParticipantTemplatesUploaded
            draftParticipants.clear()

            registerParticipantUseCase.registerParticipant(registerParticipant)
            //no sync error logged
            verify { syncLogger wasNot Called }
            draftParticipants.size.shouldBe(1)
            draftParticipants[0].draftState.shouldBe(DraftState.UPLOADED)
            draftParticipants[0].biometricsTemplate?.draftState.shouldBe(DraftState.UPLOADED)
        }
        test("participant uploaded failed") {
            coEvery { findParticipantByParticipantIdUseCase.findByParticipantId(participantUuid) } returns null
            coEvery { uploadDraftParticipantUseCase.upload(any(), allowDuplicate = false, updateDraftState = false) } throws NoNetworkException()
            draftParticipants.clear()

            registerParticipantUseCase.registerParticipant(registerParticipant)
            //no sync error logged
            verify { syncLogger wasNot Called }
            //draft participant is saved, uploadstate is pending
            draftParticipants.size.shouldBe(1)
            draftParticipants[0].draftState.shouldBe(DraftState.UPLOAD_PENDING)
            draftParticipants[0].biometricsTemplate?.draftState.shouldBe(DraftState.UPLOAD_PENDING)
        }

        test("duplicate local participant") {
            coEvery { findParticipantByParticipantIdUseCase.findByParticipantId(participantUuid) } returns uploadedDraftParticipantTemplatesUploaded
            draftParticipants.clear()

            var exception: Exception? = null
            try {
                registerParticipantUseCase.registerParticipant(registerParticipant)
            } catch (e: Exception) {
                exception = e
            }

            verify { uploadDraftParticipantUseCase wasNot Called }

            exception.shouldNotBeNull()
            exception.shouldBeTypeOf<ParticipantAlreadyExistsException>()
        }

        test("duplicate online participant") {
            coEvery { findParticipantByParticipantIdUseCase.findByParticipantId(participantUuid) } returns null
            coEvery { uploadDraftParticipantUseCase.upload(any(), any(), any()) } throws ParticipantAlreadyExistsException()
            coEvery { participantDataFileIO.deleteParticipantDataFile(any()) } returns true
            draftParticipants.clear()

            var exception: Exception? = null
            try {
                registerParticipantUseCase.registerParticipant(registerParticipant)
            } catch (e: Exception) {
                exception = e
            }

            exception.shouldNotBeNull()
            exception.shouldBeTypeOf<ParticipantAlreadyExistsException>()

            //participant is not saved locally and files are removed
            verify { draftParticipantRepository wasNot Called }
            coVerify(exactly = 2) { participantDataFileIO.deleteParticipantDataFile(any()) }


        }
    }
})

private fun createDraftParticipantRepository(draftParticipants: MutableList<DraftParticipant>): DraftParticipantRepository {
    val repo: DraftParticipantRepository = mockk()

    coEvery { repo.insert(any(), orReplace = false) } coAnswers {
        val participant = invocation.args.first() as DraftParticipant
        draftParticipants.add(participant)
    }
    return repo
}


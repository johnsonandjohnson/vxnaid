package com.jnj.vaccinetracker.common.data.managers

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.UpdateVisit
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.domain.usecases.GetParticipantVisitDetailsUseCase
import com.jnj.vaccinetracker.common.domain.usecases.GetUpcomingVisitUseCase
import com.jnj.vaccinetracker.common.domain.usecases.UpdateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
class VisitManager @Inject constructor(
    private val userRepository: UserRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val getParticipantVisitDetailsUseCase: GetParticipantVisitDetailsUseCase,
    private val updateVisitUseCase: UpdateVisitUseCase,
    private val createVisitUseCase: CreateVisitUseCase,
    private val getUpcomingVisitUseCase: GetUpcomingVisitUseCase,
) {

    suspend fun getVisitsForParticipant(participantUuid: String): List<VisitDetail> = getParticipantVisitDetailsUseCase.getParticipantVisitDetails(participantUuid)

    suspend fun registerDosingVisit(
        participantUuid: String,
        encounterDatetime: Date,
        visitUuid: String,
        vialCode: String,
        manufacturer: String,
        dosingNumber: Int,
        weight: Int,
        height: Int,
    ) {
        val locationUuid = syncSettingsRepository.getSiteUuid() ?: throw NoSiteUuidAvailableException("Trying to register dosing visit without a selected site")
        val operatorUUid = userRepository.getUser()?.uuid ?: throw OperatorUuidNotAvailableException("trying to register dosing visit without stored operator uuid")
        val attributes = mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_OCCURRED,
            Constants.ATTRIBUTE_OPERATOR to operatorUUid,
            Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to dosingNumber.toString(),
            Constants.ATTRIBUTE_VISIT_WEIGHT to weight.toString(),
            Constants.ATTRIBUTE_VISIT_HEIGHT to height.toString(),
        )

        val obs = mapOf(
            Constants.OBSERVATION_TYPE_BARCODE to vialCode,
            Constants.OBSERVATION_TYPE_MANUFACTURER to manufacturer)

        val request = UpdateVisit(
            visitUuid = visitUuid,
            startDatetime = encounterDatetime,
            participantUuid = participantUuid,
            locationUuid = locationUuid,
            attributes = attributes,
            observations = obs,
        )
        updateVisitUseCase.updateVisit(request)
    }

    suspend fun registerOtherVisit(participantUuid: String) {
        val locationUuid = syncSettingsRepository.getSiteUuid() ?: throw NoSiteUuidAvailableException("Trying to register other visit without a selected site")
        val operatorUUid = userRepository.getUser()?.uuid ?: throw OperatorUuidNotAvailableException("trying to register other visit without stored operator uuid")
        val request = CreateVisit(
            participantUuid = participantUuid,
            visitType = Constants.VISIT_TYPE_OTHER,
            startDatetime = Date(),
            locationUuid = locationUuid,
            attributes = mapOf(
                Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_OCCURRED,
                Constants.ATTRIBUTE_OPERATOR to operatorUUid,
            )
        )
        createVisitUseCase.createVisit(request)
    }

    suspend fun getUpcomingVisit(participantUuid: String): UpcomingVisit? = getUpcomingVisitUseCase.getUpcomingVisit(participantUuid, date = dateNow())

}
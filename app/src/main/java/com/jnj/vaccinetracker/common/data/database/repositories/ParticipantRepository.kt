package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.ParticipantAddressDao
import com.jnj.vaccinetracker.common.data.database.daos.ParticipantAttributeDao
import com.jnj.vaccinetracker.common.data.database.daos.ParticipantDao
import com.jnj.vaccinetracker.common.data.database.daos.base.deleteByParticipantUuid
import com.jnj.vaccinetracker.common.data.database.daos.base.findAllByPhoneNullable
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantAttributeEntity.Companion.toParticipantAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantEntity
import com.jnj.vaccinetracker.common.data.database.entities.base.toMap
import com.jnj.vaccinetracker.common.data.database.mappers.toDomain
import com.jnj.vaccinetracker.common.data.database.mappers.toPersistence
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel.Companion.toDomain
import com.jnj.vaccinetracker.common.data.database.models.RoomParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.ParticipantRepositoryCommon
import com.jnj.vaccinetracker.common.data.database.repositories.base.SyncRepositoryBase
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import javax.inject.Inject

class ParticipantRepository @Inject constructor(
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val participantDao: ParticipantDao,
    private val participantAddressDao: ParticipantAddressDao,
    private val participantAttributeDao: ParticipantAttributeDao,
    private val participantImageRepository: ParticipantImageRepository,
    private val participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository,
) : ParticipantRepositoryCommon<Participant>, SyncRepositoryBase {

    private fun RoomParticipantModel.toDomain(imageFile: ParticipantImageFile?, templateFile: ParticipantBiometricsTemplateFile?) = Participant(
        participantUuid = participantUuid,
        participantId = participantId,
        nin = nin,
        gender = gender,
        birthDate = birthDate,
        address = address?.toDomain(),
        dateModified = dateModified,
        attributes = attributes.toMap()
            .withPhone(phone)
            .withLocationUuid(locationUuid)
            .withIsBirthDayAnApproximation(isBirthDateEstimated),
        biometricsTemplate = templateFile,
        image = imageFile
    )

    private fun Participant.toPersistence() = ParticipantEntity(
        participantUuid = participantUuid,
        phone = phone,
        participantId = participantId,
        nin = nin,
        gender = gender,
        birthDate = birthDate,
        dateModified = dateModified,
        locationUuid = locationUuid,
        isBirthDateEstimated = isBirthDateEstimated,
    )

    override suspend fun findAllByPhone(phone: String?): List<Participant> {
        return participantDao.findAllByPhoneNullable(phone).map { it.toDomain() }
    }

    private suspend fun RoomParticipantModel.toDomain(): Participant {
        val image = participantImageRepository.findByParticipantUuid(participantUuid)
        val template = participantBiometricsTemplateRepository.findByParticipantUuid(participantUuid)
        return toDomain(imageFile = image, templateFile = template)
    }

    override suspend fun findMostRecentDateModifiedOccurrence(): DateModifiedOccurrence? = participantDao.findMostRecentDateModifiedOccurrence().toDomain()

    override suspend fun deleteAll() {
        participantDao.deleteAll()
    }

    override suspend fun findByParticipantId(participantId: String): Participant? {
        return participantDao.findByParticipantId(participantId)?.toDomain()
    }

    suspend fun deleteByParticipantUuid(participantUuid: String): Int {
        return participantDao.delete(RoomDeleteParticipantModel(participantUuid)).also { countDeleted ->
            logDebug("deleteByParticipantUuid: $participantUuid $countDeleted")
        }
    }

    override suspend fun findByParticipantUuid(participantUuid: String): Participant? = participantDao.findByParticipantUuid(participantUuid)?.toDomain()

    override suspend fun insert(model: Participant, orReplace: Boolean) = transactionRunner.withTransaction {
        try {
            if (orReplace) {
                //we assume due to foreign keys, the related child rows will be deleted as well
                val isDeleted = participantDao.deleteByParticipantUuid(model.participantUuid) > 0
                if (isDeleted) {
                    logInfo("deleted participant for replace ${model.participantUuid}")
                }
            }

            val insertedParticipant = participantDao.insert(model.toPersistence()) > 0
            if (!insertedParticipant) {
                throw InsertEntityException("Cannot save participant: ${model.participantUuid}", orReplace = orReplace)
            }

            val attributes = model.attributes.map { it.toParticipantAttributeEntity(model.participantUuid) }
            if (attributes.isNotEmpty()) {
                val insertedAttributes = participantAttributeDao.insertOrReplaceAll(attributes)
                    .all { it > 0 }
                if (!insertedAttributes) {
                    throw InsertEntityException("Cannot save attributes for participant: ${model.participantUuid}", orReplace = orReplace)
                }
            }

            val address = model.address?.toPersistence(model.participantUuid)
            if (address != null) {
                val insertedAddress = participantAddressDao.insertOrReplace(address) > 0
                if (!insertedAddress) {
                    throw InsertEntityException("Cannot save address for participant: ${model.participantUuid}", orReplace = orReplace)
                }
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save participant", orReplace = orReplace)
        }
    }

    override fun observeChanges(): Flow<Long> {
        return participantDao.observeChanges()
    }

    override suspend fun findRegimen(participantUuid: String): String? {
        return participantAttributeDao.findAttribute(participantUuid, type = Constants.ATTRIBUTE_VACCINE)
    }
}
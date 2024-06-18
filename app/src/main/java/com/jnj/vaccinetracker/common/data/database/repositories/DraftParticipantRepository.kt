package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.base.deleteByParticipantUuid
import com.jnj.vaccinetracker.common.data.database.daos.base.findAllByPhoneNullable
import com.jnj.vaccinetracker.common.data.database.daos.base.updateDraftStateOrThrow
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftParticipantAddressDao
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftParticipantAttributeDao
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftParticipantDao
import com.jnj.vaccinetracker.common.data.database.entities.base.toMap
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantAttributeEntity.Companion.toDraftParticipantAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantEntity
import com.jnj.vaccinetracker.common.data.database.mappers.toDomain
import com.jnj.vaccinetracker.common.data.database.mappers.toDraftPersistence
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateParticipantDraftStateModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.DeleteByDraftParticipant
import com.jnj.vaccinetracker.common.data.database.repositories.base.DraftParticipantRepositoryBase
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import javax.inject.Inject

class DraftParticipantRepository @Inject constructor(
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val draftParticipantDao: DraftParticipantDao,
    private val draftParticipantAddressDao: DraftParticipantAddressDao,
    private val draftParticipantAttributeDao: DraftParticipantAttributeDao,
    private val draftParticipantImageRepository: DraftParticipantImageRepository,
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
) : DraftParticipantRepositoryBase<DraftParticipant>, DeleteByDraftParticipant {

    private fun RoomDraftParticipantModel.toDomain(imageFile: DraftParticipantImageFile?, templateFile: DraftParticipantBiometricsTemplateFile?) = DraftParticipant(
        participantUuid = participantUuid,
        image = imageFile,
        biometricsTemplate = templateFile,
        participantId = participantId,
        nin = nin,
        gender = gender,
        birthDate = birthDate,
        address = address?.toDomain(),
        registrationDate = registrationDate,
        attributes = attributes.toMap()
            .withPhone(phone)
            .withLocationUuid(locationUuid)
            .withBirthWeight(birthWeight)
            .withIsBirthDateEstimated(isBirthDateEstimated),
            // TODO add the map of birthweight here
        draftState = draftState
    )

    private fun DraftParticipant.toPersistence() = DraftParticipantEntity(
        participantUuid = participantUuid,
        phone = phone,
        participantId = participantId,
        nin = nin,
        gender = gender,
        birthDate = birthDate,
        birthWeight = birthWeight,
        isBirthDateEstimated = isBirthDateEstimated,
        draftState = DraftState.initialState(),
        registrationDate = dateModified,
        locationUuid = locationUuid
    )

    override suspend fun findAllByPhone(phone: String?): List<DraftParticipant> {
        return draftParticipantDao.findAllByPhoneNullable(phone).map { it.toDomain() }
    }

    override suspend fun findAllParticipantUuidsByDraftState(draftState: DraftState, offset: Int, limit: Int): List<String> {
        return draftParticipantDao.findAllParticipantUuidsByDraftState(draftState, offset, limit).map { it.participantUuid }
    }

    override suspend fun findByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): DraftParticipant? {
        return draftParticipantDao.findByParticipantUuidAndDraftState(participantUuid, draftState)?.toDomain()
    }

    /**
     * WARNING:will not update template or image
     */
    suspend fun updateDraftParticipant(draft: DraftParticipant) {
        logInfo("updateDraftParticipant ${draft.participantUuid}")
        draftParticipantDao.update(draft.toPersistence())
    }

    suspend fun updateDraftState(draft: DraftParticipant, updateImage: Boolean, updateBiometricsTemplate: Boolean) = transactionRunner.withTransaction {
        logInfo("updateDraftState: ${draft.participantUuid} updateImage:$updateImage updateBiometricsTemplate:$updateBiometricsTemplate")
        if (updateImage) {
            draft.image?.let { draftParticipantImageRepository.updateDraftState(it) }
        }
        if (updateBiometricsTemplate) {
            draft.biometricsTemplate?.let { draftParticipantBiometricsTemplateRepository.updateDraftState(it) }
        }
        updateDraftState(draft)
    }

    override suspend fun updateDraftState(draft: DraftParticipant) {
        val updateModel = RoomUpdateParticipantDraftStateModel(participantUuid = draft.participantUuid, draftState = draft.draftState)
        draftParticipantDao.updateDraftStateOrThrow(updateModel)
    }


    suspend fun delete(draft: DraftParticipant, deleteImage: Boolean, deleteTemplate: Boolean) = transactionRunner.withTransaction {
        logInfo("deleting draft ${draft.participantUuid} deleteImage:$deleteImage deleteTemplate:$deleteTemplate")
        if (deleteImage) {
            draft.image?.let { draftParticipantImageRepository.deleteByParticipantUuid(it.participantUuid) }
        }
        if (deleteTemplate) {
            draft.biometricsTemplate?.let { draftParticipantBiometricsTemplateRepository.deleteByParticipantUuid(it.participantUuid) }
        }
        deleteByParticipantUuid(draft.participantUuid)
    }

    /**
     * does not delete image or template
     */
    override suspend fun deleteByParticipantUuid(participantUuid: String): Boolean {
        val success = draftParticipantDao.deleteByParticipantUuid(participantUuid) > 0
        logInfo("deleteByParticipantId: $success")
        return success
    }

    override fun observeChanges(): Flow<Long> {
        return draftParticipantDao.observeChanges()
    }

    private suspend fun RoomDraftParticipantModel.toDomain(): DraftParticipant {
        val image = draftParticipantImageRepository.findByParticipantUuid(participantUuid)
        val template = draftParticipantBiometricsTemplateRepository.findByParticipantUuid(participantUuid)
        return toDomain(imageFile = image, templateFile = template)
    }

    override suspend fun findByParticipantId(participantId: String): DraftParticipant? {
        return draftParticipantDao.findByParticipantId(participantId)?.toDomain()
    }

    suspend fun findDraftStateByParticipantUuid(participantUuid: String): DraftState? {
        return draftParticipantDao.findDraftStateByParticipantUuid(participantUuid)
    }

    override suspend fun findByParticipantUuid(participantUuid: String): DraftParticipant? {
        return draftParticipantDao.findByParticipantUuid(participantUuid)?.toDomain()
    }

    override suspend fun insert(model: DraftParticipant, orReplace: Boolean) = transactionRunner.withTransaction {
        model.image?.let {
            draftParticipantImageRepository.insert(it, orReplace = orReplace)
        }
        model.biometricsTemplate?.let {
            draftParticipantBiometricsTemplateRepository.insert(it, orReplace = orReplace)
        }
        try {
            if (orReplace) {
                //we assume due to foreign keys, the related child rows will be deleted as well
                val isDeleted = draftParticipantDao.deleteByParticipantUuid(model.participantUuid) > 0
                if (isDeleted) {
                    logInfo("deleted draft participant for replace ${model.participantUuid}")
                }
            }

            val insertedParticipant = draftParticipantDao.insert(model.toPersistence()) > 0
            if (!insertedParticipant) {
                throw InsertEntityException("Cannot save draft participant: ${model.participantUuid}", orReplace = orReplace)
            }

            val attributes = model.attributes.map { it.toDraftParticipantAttributeEntity(model.participantUuid) }
            if (attributes.isNotEmpty()) {
                val insertedAttributes = draftParticipantAttributeDao.insertOrReplaceAll(attributes)
                    .all { it > 0 }
                if (!insertedAttributes) {
                    throw InsertEntityException("Cannot save attributes for draft participant: ${model.participantUuid}", orReplace = orReplace)
                }
            }

            val address = model.address?.toDraftPersistence(model.participantUuid)
            if (address != null) {
                val insertedAddress = draftParticipantAddressDao.insertOrReplace(address) > 0
                if (!insertedAddress) {
                    throw InsertEntityException("Cannot save address for draft participant: ${model.participantUuid}", orReplace = orReplace)
                }
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save draft participant", orReplace = orReplace)
        }
    }

    override suspend fun deleteAllUploaded() {
        val countDeleted = draftParticipantDao.deleteAllByDraftState(DraftState.UPLOADED)
        logInfo("deleteAllUploaded: countDeleted $countDeleted")
    }

    override suspend fun countByDraftState(draftState: DraftState): Long {
        return draftParticipantDao.countByDraftState(draftState)
    }

    override suspend fun findRegimen(participantUuid: String): String? {
        return draftParticipantAttributeDao.findAttribute(participantUuid, type = Constants.ATTRIBUTE_VACCINE)
    }
}
package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.data.database.entities.base.SyncBase
import com.jnj.vaccinetracker.common.data.database.entities.base.UploadableDraft
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.models.Constants

sealed class ParticipantBase {
    abstract val participantUuid: String
    abstract val nin: String?
    abstract val image: ParticipantImageFileBase?
    abstract val biometricsTemplate: ParticipantBiometricsTemplateFileBase?
    abstract val participantId: String
    abstract val gender: Gender
    abstract val birthDate: BirthDate
    abstract val attributes: Map<String, String>
    abstract val address: Address?

    val phone: String? get() = attributes[Constants.ATTRIBUTE_TELEPHONE]
    val locationUuid: String? get() = attributes[Constants.ATTRIBUTE_LOCATION]
    val originalParticipantId: String? get() = attributes[Constants.ATTRIBUTE_ORIGINAL_PARTICIPANT_ID]
    val regimen: String? get() = attributes[Constants.ATTRIBUTE_VACCINE]
}

data class Participant(
    override val participantUuid: String,
    override val dateModified: DateEntity,
    override val image: ParticipantImageFile?,
    override val biometricsTemplate: ParticipantBiometricsTemplateFile?,
    override val participantId: String,
    override val nin: String?,
    override val gender: Gender,
    override val birthDate: BirthDate,
    override val attributes: Map<String, String>,
    override val address: Address?,
) : ParticipantBase(), SyncBase

data class DraftParticipant(
    override val participantUuid: String,
    val registrationDate: DateEntity,
    override val image: DraftParticipantImageFile?,
    override val biometricsTemplate: DraftParticipantBiometricsTemplateFile?,
    override val participantId: String,
    override val nin: String?,
    override val gender: Gender,
    override val birthDate: BirthDate,
    override val attributes: Map<String, String>,
    override val address: Address?,
    override val draftState: DraftState,
) : ParticipantBase(), SyncBase, UploadableDraft {
    override val dateModified: DateEntity get() = registrationDate
}

fun Map<String, String>.withOriginalParticipantId(participantId: String?): Map<String, String> {
    val participantIdKey = Constants.ATTRIBUTE_ORIGINAL_PARTICIPANT_ID
    return participantId?.let { this + mapOf(participantId to it) } ?: filterKeys { it != participantIdKey }
}

fun Map<String, String>.withPhone(phone: String?): Map<String, String> {
    val phoneKey = Constants.ATTRIBUTE_TELEPHONE
    return phone?.let { this + mapOf(phoneKey to it) } ?: filterKeys { it != phoneKey }
}

fun Map<String, String>.withLocationUuid(locationUuid: String?): Map<String, String> {
    val locationKey = Constants.ATTRIBUTE_LOCATION
    return locationUuid?.let { this + mapOf(locationKey to it) } ?: filterKeys { it != locationKey }
}

fun DraftParticipant.toParticipantWithoutAssets(): Participant = Participant(
    participantUuid = participantUuid,
    dateModified = dateModified,
    image = null,
    biometricsTemplate = null,
    participantId = participantId,
    nin = nin,
    gender = gender,
    birthDate = birthDate,
    attributes = attributes,
    address = address
)

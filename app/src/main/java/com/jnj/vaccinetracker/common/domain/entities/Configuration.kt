package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.sync.data.models.SyncScopeLevel
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @author maartenvangiel
 * @author druelens
 * @version 1
 */
@JsonClass(generateAdapter = true)
data class Configuration(
    val syncScope: SyncScopeLevel,
    val operatorCredentialsRetentionTime: Long, // Time to retain operator credentials for offline use, in milliseconds
    val operatorOfflineSessionTimeout: Long,
    @Json(name = "vaccine") val vaccines: List<Vaccine>,
    val canUseDifferentManufacturers: Boolean,
    val manufacturers: List<Manufacturer>,
    val personLanguages: List<NamedValue>,
    val authSteps: List<IdentificationStep>,
    val irisScore: Int,
    val addressFields: Map<String, List<AddressField>>, // contents: <country, addressValues>
    val allowManualParticipantIDEntry: Boolean = true,
    val participantIDRegex: String? = null,
) {
    val isAutoGenerateParticipantId: Boolean get() = !allowManualParticipantIDEntry
}

@JsonClass(generateAdapter = true)
data class NamedValue(val name: String)

@JsonClass(generateAdapter = true)
data class AddressField(
    val field: AddressValueType,
    @Json(name = "type")
    val inputType: InputType,
    val name: String,
    val displayOrder: Int,
) {
    enum class InputType {
        DROPDOWN, FREE_INPUT
    }
}

@JsonClass(generateAdapter = true)
data class IdentificationStep(
    val type: String,
    val mandatory: Boolean,
)

@JsonClass(generateAdapter = true)
data class Vaccine(
    val name: String,
    val manufacturers: List<String>,
)

@JsonClass(generateAdapter = true)
data class Manufacturer(
    val name: String,
    val barcodeRegex: String,
)
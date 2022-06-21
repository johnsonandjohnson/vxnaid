package com.jnj.vaccinetracker.sync.data.models

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.sync.domain.entities.SyncError
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorBase
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncErrorDto(
    override val metadata: SyncErrorMetadata,
    override val stackTrace: String,
    override val dateCreated: DateEntity,
    override val key: String = metadata.key,
) :
    SyncErrorBase

fun SyncError.toDto() = SyncErrorDto(metadata = metadata, stackTrace = stackTrace, dateCreated = dateCreated)

@JsonClass(generateAdapter = true)
data class SyncErrorsDto(
    val jsonGenerationDate: DateEntity,
    val deviceInfo: SyncErrorsDeviceInfo,
    val syncErrors: List<SyncErrorDto>,
)

@JsonClass(generateAdapter = true)
data class SyncErrorsDeviceInfo(
    val deviceId: String,
    val deviceName: String?,
    val siteUuid: String?,
    val backendUrl: String,
    val appVersion: String,
    val deviceHardware: DeviceHardware,
)

@JsonClass(generateAdapter = true)
data class DeviceHardware(val androidVersion: String, val model: String, val device: String, val product: String)
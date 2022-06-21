package com.jnj.vaccinetracker.fake.data.random

import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.fake.data.network.MockAssetReader
import com.jnj.vaccinetracker.sync.data.models.ParticipantBiometricsTemplateSyncRecord
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import javax.inject.Inject

class RandomBiometricsTemplateGenerator @Inject constructor(private val mockAssetReader: MockAssetReader, private val base64: Base64) {

    suspend fun generateTemplate(participantUuid: String, dateModified: SyncDate): ParticipantBiometricsTemplateSyncRecord {
        return ParticipantBiometricsTemplateSyncRecord.Update(participantUuid, dateModified, base64.encode(mockAssetReader.readRandomIrisTemplate()))
    }
}
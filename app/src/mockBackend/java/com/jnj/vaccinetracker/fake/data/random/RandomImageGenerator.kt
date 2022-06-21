package com.jnj.vaccinetracker.fake.data.random

import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.fake.data.network.MockAssetReader
import com.jnj.vaccinetracker.sync.data.models.ParticipantImageSyncRecord
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import javax.inject.Inject

class RandomImageGenerator @Inject constructor(private val mockAssetReader: MockAssetReader, private val base64: Base64) {

    suspend fun generateImage(participantUUid: String, dateModified: SyncDate): ParticipantImageSyncRecord {
        return ParticipantImageSyncRecord.Update(participantUUid, dateModified, base64.encode(mockAssetReader.readImage()))
    }
}
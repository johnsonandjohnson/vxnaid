package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantImageRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantImageRepository
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.domain.entities.ImageBytes
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetPersonImageUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val base64: Base64,
    private val dispatchers: AppCoroutineDispatchers,
    private val participantDataFileIO: ParticipantDataFileIO,
    private val draftParticipantImageRepository: DraftParticipantImageRepository,
    private val participantImageRepository: ParticipantImageRepository,
) {

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun getPersonImageRemote(participantUuid: String): ImageBytes? = withContext(dispatchers.io) {
        try {
            api.getPersonImage(participantUuid)
                ?.string()
                ?.let { base64.decode(it) }
                ?.let { ImageBytes((it)) }
        } catch (ex: NoNetworkException) {
            logInfo("getPersonImageRemote: no network so returning NULL")
            null
        }
    }

    private suspend fun getPersonImageLocal(participantUuid: String): ImageBytes? {
        return try {
            participantImageRepository.findByParticipantUuid(participantUuid)?.let { file ->
                participantDataFileIO.readParticipantDataFileContent(file)
            }?.let { ImageBytes(it) }
        } catch (throwable: Throwable) {
            logError("getPersonImageLocal $participantUuid error", throwable)
            null
        }
    }

    private suspend fun getPersonImageLocalDraft(participantUuid: String): ImageBytes? {
        return try {
            draftParticipantImageRepository.findByParticipantUuid(participantUuid)?.let { file ->
                participantDataFileIO.readParticipantDataFileContent(file)
            }?.let { ImageBytes(it) }
        } catch (throwable: Throwable) {
            logError("getPersonImageLocalDraft $participantUuid error", throwable)
            null
        }
    }


    suspend fun getPersonImage(participantUuid: String): ImageBytes? = withContext(dispatchers.io) {
        val remoteImage = async(dispatchers.io) {
            kotlin.runCatching {
                getPersonImageRemote(participantUuid)
            }

        }
        getPersonImageLocal(participantUuid) ?: getPersonImageLocalDraft(participantUuid) ?: remoteImage.await().getOrThrow()
    }
}
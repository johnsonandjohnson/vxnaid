package com.jnj.vaccinetracker.sync.p2p.domain.usecases

import com.jnj.vaccinetracker.common.data.database.ParticipantRoomDatabase
import com.jnj.vaccinetracker.common.data.database.ParticipantRoomDatabaseConfig
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import javax.inject.Inject

class PrepareDatabaseForExportUseCase @Inject constructor(
    private val participantRoomDatabaseFactory: ParticipantRoomDatabase.Factory,
) {
    /**
     * this will delete all records that should stay private to this device
     */
    suspend fun prepareDatabaseForExport(dbName: String) {
        if (dbName == ParticipantRoomDatabaseConfig.FILE_NAME) {
            error("dbName==ParticipantRoomDatabaseConfig.FILE_NAME")
        }
        val database = participantRoomDatabaseFactory.createDatabaseWithDefaultPassphrase(dbName, deleteDatabaseIfCorrupt = false)
        val pendingUpload = DraftState.UPLOAD_PENDING
        // delete sync errors
        database.syncErrorDao().deleteAll()
        // delete not uploaded encounters
        database.draftVisitEncounterDao().deleteAllByDraftState(pendingUpload)
        // delete not uploaded visits
        database.draftVisitDao().deleteAllByDraftState(pendingUpload)
        // delete not uploaded images
        database.draftParticipantImageDao().deleteAllByDraftState(pendingUpload)
        // delete not uploaded templates
        database.draftParticipantBiometricsTemplateDao().deleteAllByDraftState(pendingUpload)
        // delete not uploaded participants
        database.draftParticipantDao().deleteAllByDraftState(pendingUpload)

        //VERY IMPORTANT: close the database so -wal file is emptied. Otherwise the changes are flushed to the main db file.
        database.close()
    }
}
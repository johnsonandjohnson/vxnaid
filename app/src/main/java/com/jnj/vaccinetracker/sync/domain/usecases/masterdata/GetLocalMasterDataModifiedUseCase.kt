package com.jnj.vaccinetracker.sync.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import javax.inject.Inject

class GetLocalMasterDataModifiedUseCase @Inject constructor(private val masterDataRepository: MasterDataRepository) {

    fun getMasterDataSyncDate(masterDataFile: MasterDataFile): SyncDate? {
        return masterDataRepository.getDateModified(masterDataFile)
    }
}
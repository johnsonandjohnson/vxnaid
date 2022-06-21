package com.jnj.vaccinetracker.sync.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.models.api.response.SitesDto
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncSitesUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository, override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<SitesDto>() {
    override suspend fun getMasterDataRemote() = api.getSites()

    override suspend fun storeMasterData(masterData: SitesDto) {
        masterDataRepository.writeSites(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.SITES
}
package com.jnj.vaccinetracker.sync.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncSubstancesGroupConfigUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<SubstancesGroupConfig>() {
    override suspend fun getMasterDataRemote(): SubstancesGroupConfig = api.getSubstancesGroupConfig()

    override suspend fun storeMasterData(masterData: SubstancesGroupConfig) {
        masterDataRepository.writeSubstancesGroupConfig(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.SUBSTANCES_GROUP_CONFIG
}
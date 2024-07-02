package com.jnj.vaccinetracker.common.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSubstancesGroupConfigUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers,
    override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<SubstancesGroupConfig, SubstancesGroupConfig>() {

    init {
        initState()
    }

    override fun getMemoryCache(): SubstancesGroupConfig? {
        return masterDataMemoryDataSource.getSubstancesGroupConfig()
    }

    override fun setMemoryCache(memoryCache: SubstancesGroupConfig?) {
        masterDataMemoryDataSource.setSubstancesGroupConfig(memoryCache)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.SUBSTANCES_GROUP_CONFIG

    override suspend fun SubstancesGroupConfig.toDomain(): SubstancesGroupConfig {
        return this
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readSubstancesGroupConfig()

    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getSubstancesGroupConfig()
}
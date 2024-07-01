package com.jnj.vaccinetracker.common.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSubstancesConfigUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers,
    override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<SubstancesConfig, SubstancesConfig>() {

    init {
        initState()
    }

    override fun getMemoryCache(): SubstancesConfig? {
        return masterDataMemoryDataSource.getSubstanceConfig()
    }

    override fun setMemoryCache(memoryCache: SubstancesConfig?) {
        masterDataMemoryDataSource.setSubstanceConfig(memoryCache)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.SUBSTANCES_CONFIG

    override suspend fun SubstancesConfig.toDomain(): SubstancesConfig {
        return this
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readSubstanceConfig()
    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getSubstancesConfig()
}
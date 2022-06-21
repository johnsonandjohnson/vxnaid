package com.jnj.vaccinetracker.common.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.jnj.vaccinetracker.common.data.models.api.response.ConfigurationDto
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.Configuration
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetConfigurationUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers,
    override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<ConfigurationDto, Configuration>() {
    init {
        initState()
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.CONFIGURATION

    override suspend fun ConfigurationDto.toDomain(): Configuration {
        return this
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readConfiguration()
    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getConfiguration()

    override fun getMemoryCache(): Configuration? {
        return masterDataMemoryDataSource.getConfiguration()
    }

    override fun setMemoryCache(memoryCache: Configuration?) {
        masterDataMemoryDataSource.setConfiguration(memoryCache)
    }
}
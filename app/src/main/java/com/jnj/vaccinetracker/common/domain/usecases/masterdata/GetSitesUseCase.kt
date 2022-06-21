package com.jnj.vaccinetracker.common.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.jnj.vaccinetracker.common.data.models.api.response.SitesDto
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.Sites
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSitesUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers, override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<SitesDto, Sites>() {
    init {
        initState()
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.SITES

    override suspend fun SitesDto.toDomain(): Sites {
        return this
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readSites()
    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getSites()

    override fun getMemoryCache(): Sites? {
        return masterDataMemoryDataSource.getSites()
    }

    override fun setMemoryCache(memoryCache: Sites?) {
        masterDataMemoryDataSource.setSites(memoryCache)
    }
}
package com.jnj.vaccinetracker.common.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.jnj.vaccinetracker.common.data.mappers.AddressHierarchyDtoMapper
import com.jnj.vaccinetracker.common.data.models.api.response.AddressHierarchyDto
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.AddressHierarchy
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAddressHierarchyUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers,
    override val syncLogger: SyncLogger,
    private val addressHierarchyDtoMapper: AddressHierarchyDtoMapper,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<AddressHierarchyDto, AddressHierarchy>() {
    init {
        initState()
        observeConfiguration()
    }

    private suspend fun onNewConfigurationLoaded() {
        logInfo("onNewConfigurationLoaded")
        // clear address hierarchy in memory
        setMemoryCache(null)
        // reload the address hierarchy
        try {
            getMasterData()
        } catch (ex: Exception) {
            ex.rethrowIfFatal()
            logError("onNewConfigurationLoaded failed to reload address hierarchy", ex)
        }
    }

    /**
     * we want to monitor configuration changes because it has impact on the address hierarchy mapping
     */
    private fun observeConfiguration() {
        syncLogger.observeMasterDataLoadedInMemory(MasterDataFile.CONFIGURATION)
            .onEach { onNewConfigurationLoaded() }
            .launchIn(scope)
    }

    override fun getMemoryCache(): AddressHierarchy? {
        return masterDataMemoryDataSource.getAddressHierarchy()
    }

    override fun setMemoryCache(memoryCache: AddressHierarchy?) {
        masterDataMemoryDataSource.setAddressHierarchy(memoryCache)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.ADDRESS_HIERARCHY

    override suspend fun AddressHierarchyDto.toDomain(): AddressHierarchy {
        return addressHierarchyDtoMapper.toDomain(this)
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readAddressHierarchy()
    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getCountryAddressHierarchy()
}
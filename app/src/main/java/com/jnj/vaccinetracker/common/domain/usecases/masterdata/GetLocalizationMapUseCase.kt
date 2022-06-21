package com.jnj.vaccinetracker.common.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.jnj.vaccinetracker.common.data.models.api.response.LocalizationMapDto
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.LanguageMap
import com.jnj.vaccinetracker.common.domain.entities.LocalizationMap
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.TranslationMap
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLocalizationMapUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers, override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<LocalizationMapDto, LocalizationMap>() {

    init {
        initState()
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.LOCALIZATION

    override suspend fun LocalizationMapDto.toDomain(): LocalizationMap {
        return LocalizationMap(LanguageMap(localization.mapValues { TranslationMap(it.value) }))
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readLocalizationMap()
    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getLocalization()

    override fun getMemoryCache(): LocalizationMap? {
        return masterDataMemoryDataSource.getLocalization()
    }

    override fun setMemoryCache(memoryCache: LocalizationMap?) {
        masterDataMemoryDataSource.setLocalization(memoryCache)
    }
}
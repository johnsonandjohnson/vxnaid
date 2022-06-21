package com.jnj.vaccinetracker.sync.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.models.api.response.LocalizationMapDto
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncLocalizationUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository, override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<LocalizationMapDto>() {
    override suspend fun getMasterDataRemote(): LocalizationMapDto {
        return api.getLocalization()
    }

    override suspend fun storeMasterData(masterData: LocalizationMapDto) {
        masterDataRepository.writeLocalizationMap(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.LOCALIZATION
}
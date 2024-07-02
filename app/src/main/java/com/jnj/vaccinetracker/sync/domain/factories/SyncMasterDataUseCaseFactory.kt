package com.jnj.vaccinetracker.sync.domain.factories

import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.sync.domain.usecases.masterdata.*
import com.jnj.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCase
import javax.inject.Inject

class SyncMasterDataUseCaseFactory @Inject constructor(
    private val syncSitesUseCase: SyncSitesUseCase,
    private val syncConfigurationUseCase: SyncConfigurationUseCase,
    private val syncLocalizationUseCase: SyncLocalizationUseCase,
    private val syncAddressHierarchyUseCase: SyncAddressHierarchyUseCase,
    private val syncVaccineScheduleUseCase: SyncVaccineScheduleUseCase,
    private val syncSubstancesConfigUseCase: SyncSubstancesConfigUseCase,
    private val syncSubstancesGroupConfigUseCase: SyncSubstancesGroupConfigUseCase
) {

    fun create(masterDataFile: MasterDataFile): SyncMasterDataUseCase {
        return when (masterDataFile) {
            MasterDataFile.CONFIGURATION -> syncConfigurationUseCase
            MasterDataFile.SITES -> syncSitesUseCase
            MasterDataFile.LOCALIZATION -> syncLocalizationUseCase
            MasterDataFile.ADDRESS_HIERARCHY -> syncAddressHierarchyUseCase
            MasterDataFile.VACCINE_SCHEDULE -> syncVaccineScheduleUseCase
            MasterDataFile.SUBSTANCES_CONFIG -> syncSubstancesConfigUseCase
            MasterDataFile.SUBSTANCES_GROUP_CONFIG -> syncSubstancesGroupConfigUseCase
        }
    }
}
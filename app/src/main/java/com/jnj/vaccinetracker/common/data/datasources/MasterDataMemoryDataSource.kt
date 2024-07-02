package com.jnj.vaccinetracker.common.data.datasources

import com.jnj.vaccinetracker.common.domain.entities.AddressHierarchy
import com.jnj.vaccinetracker.common.domain.entities.Configuration
import com.jnj.vaccinetracker.common.domain.entities.LocalizationMap
import com.jnj.vaccinetracker.common.domain.entities.Sites
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.jnj.vaccinetracker.sync.data.models.VaccineSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterDataMemoryDataSource @Inject constructor() {
    /**
     * used to avoid CPU intensive equals call
     */
    private class Container<T>(val obj: T)

    private val sitesFlow = MutableStateFlow<Container<Sites?>>(Container(null))
    private val configurationFlow = MutableStateFlow<Container<Configuration?>>(Container(null))
    private val localizationFlow = MutableStateFlow<Container<LocalizationMap?>>(Container(null))
    private val addressHierarchyFlow = MutableStateFlow<Container<AddressHierarchy?>>(Container(null))
    private val vaccineScheduleFlow = MutableStateFlow<Container<VaccineSchedule?>>(Container(null))
    private val substancesConfigFlow = MutableStateFlow<Container<SubstancesConfig?>>(Container(null))
    private val substancesGroupConfigFlow = MutableStateFlow<Container<SubstancesGroupConfig?>>(Container(null))

    fun setSites(sites: Sites?) {
        sitesFlow.value = Container(sites)
    }

    fun getSites(): Sites? = sitesFlow.value.obj


    fun observeSites(): Flow<Sites?> = sitesFlow.map { it.obj }

    fun setConfiguration(configuration: Configuration?) {
        configurationFlow.value = Container(configuration)
    }

    fun getConfiguration(): Configuration? = configurationFlow.value.obj

    fun observeConfiguration(): Flow<Configuration?> = configurationFlow.map { it.obj }

    fun setLocalization(localization: LocalizationMap?) {
        localizationFlow.value = Container(localization)
    }

    fun getLocalization(): LocalizationMap? = localizationFlow.value.obj

    fun observeLocalization(): Flow<LocalizationMap?> = localizationFlow.map { it.obj }

    fun setAddressHierarchy(addressHierarchy: AddressHierarchy?) {
        addressHierarchyFlow.value = Container(addressHierarchy)
    }

    fun getAddressHierarchy(): AddressHierarchy? = addressHierarchyFlow.value.obj

    fun observeAddressHierarchy(): Flow<AddressHierarchy?> = addressHierarchyFlow.map { it.obj }

    fun clear() {
        setSites(null)
        setLocalization(null)
        setAddressHierarchy(null)
        setConfiguration(null)
        setVaccineSchedule(null)
    }

    fun setVaccineSchedule(vaccineSchedule: VaccineSchedule?) {
        vaccineScheduleFlow.value = Container(vaccineSchedule)
    }

    fun getVaccineSchedule(): VaccineSchedule? = vaccineScheduleFlow.value.obj

    fun observeVaccineSchedule(): Flow<VaccineSchedule?> = vaccineScheduleFlow.map { it.obj }

    fun setSubstanceConfig(substanceConfig: SubstancesConfig?) {
        substancesConfigFlow.value = Container(substanceConfig)
    }

    fun getSubstanceConfig(): SubstancesConfig? = substancesConfigFlow.value.obj

    fun observeSubstanceConfig(): Flow<SubstancesConfig?> = substancesConfigFlow.map { it.obj }

    fun setSubstancesGroupConfig(substancesGroupConfig: SubstancesGroupConfig?) {
        substancesGroupConfigFlow.value = Container(substancesGroupConfig)
    }

    fun getSubstancesGroupConfig(): SubstancesGroupConfig? = substancesGroupConfigFlow.value.obj

    fun observeSubstancesGroupConfig(): Flow<SubstancesGroupConfig?> = substancesGroupConfigFlow.map { it.obj }
}
package com.jnj.vaccinetracker.common.di

import com.jnj.vaccinetracker.common.NetworkConnectivityFake
import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiDataSource
import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiDataSourceDefault
import com.jnj.vaccinetracker.common.helpers.NetworkConnectivity
import com.jnj.vaccinetracker.common.helpers.NetworkConnectivityDefault
import com.jnj.vaccinetracker.config.mockAppSettings
import com.jnj.vaccinetracker.fake.data.network.VaccineTrackerApiDataSourceFake
import com.jnj.vaccinetracker.fake.data.network.VaccineTrackerSyncApiDataSourceFake
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSourceDefault
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import javax.inject.Singleton

@Module(includes = [MockSettingsDialogModule::class])
class ApiDataSourceModule {

    @Provides
    @Singleton
    fun provideVaccineTrackerSyncApiDataSource(
        implFake: Provider<VaccineTrackerSyncApiDataSourceFake>,
        implDefault: Provider<VaccineTrackerSyncApiDataSourceDefault>,
    ): VaccineTrackerSyncApiDataSource = if (mockAppSettings.useFakeSyncApiDataSource) implFake.get() else implDefault.get()

    @Provides
    @Singleton
    fun provideVaccineTrackerApiDataSource(
        implFake: Provider<VaccineTrackerApiDataSourceFake>,
        implDefault: Provider<VaccineTrackerApiDataSourceDefault>,
    ): VaccineTrackerApiDataSource = if (mockAppSettings.useFakeApiDataSource) implFake.get() else implDefault.get()

    @Provides
    @Singleton
    fun provideNetworkConnectivity(
        implFake: Provider<NetworkConnectivityFake>,
        implDefault: Provider<NetworkConnectivityDefault>,
    ): NetworkConnectivity = if (mockAppSettings.useFakeNetworkConnectivity) implFake.get() else implDefault.get()
}
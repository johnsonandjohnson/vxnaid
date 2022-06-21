package com.jnj.vaccinetracker.common.di

import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiDataSource
import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiDataSourceDefault
import com.jnj.vaccinetracker.common.helpers.NetworkConnectivity
import com.jnj.vaccinetracker.common.helpers.NetworkConnectivityDefault
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSourceDefault
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApiDataSourceModule {

    @Provides
    @Singleton
    fun provideVaccineTrackerSyncApiDataSource(
        impl: VaccineTrackerSyncApiDataSourceDefault,
    ): VaccineTrackerSyncApiDataSource = impl

    @Provides
    @Singleton
    fun provideVaccineTrackerApiDataSource(
        impl: VaccineTrackerApiDataSourceDefault,
    ): VaccineTrackerApiDataSource = impl

    @Provides
    @Singleton
    fun provideNetworkConnectivity(impl: NetworkConnectivityDefault): NetworkConnectivity = impl
}
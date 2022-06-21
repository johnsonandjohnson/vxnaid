package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.domain.entities.VaccineTrackerVersion
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import javax.inject.Inject

class GetLatestVersionUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
) {
    suspend fun getLatestVersion(): VaccineTrackerVersion = api.getLatestVersion()
}
package com.jnj.vaccinetracker.sync.domain.usecases.syncscope

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.domain.usecases.GetSelectedSiteUseCase
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import com.jnj.vaccinetracker.sync.domain.entities.SyncScope
import javax.inject.Inject

class BuildSyncScopeUseCase @Inject constructor(
    private val getConfigurationUseCase: GetConfigurationUseCase,
    private val getSelectedSiteUseCase: GetSelectedSiteUseCase,
) {
    suspend fun buildSyncScope(): SyncScope {
        val selectedSite = getSelectedSiteUseCase.getSelectedSite()
        val config = getConfigurationUseCase.getMasterData()
        val scopeLevel = config.syncScope
        return SyncScope(
            siteUuid = selectedSite.uuid,
            level = scopeLevel,
            dateCreated = dateNow(),
            cluster = selectedSite.cluster,
            country = selectedSite.country)
    }
}
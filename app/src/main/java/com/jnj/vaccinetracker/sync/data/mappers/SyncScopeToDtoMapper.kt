package com.jnj.vaccinetracker.sync.data.mappers

import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetSitesUseCase
import com.jnj.vaccinetracker.common.exceptions.SiteNotFoundException
import com.jnj.vaccinetracker.sync.data.models.SyncScopeDto
import com.jnj.vaccinetracker.sync.data.models.toSyncScopeDto
import com.jnj.vaccinetracker.sync.domain.entities.SyncScope
import javax.inject.Inject

class SyncScopeToDtoMapper @Inject constructor(private val getSitesUseCase: GetSitesUseCase) {

    suspend fun toDto(syncScope: SyncScope): SyncScopeDto {
        val sites = getSitesUseCase.getMasterData()
        val site = sites.results.find { it.uuid == syncScope.siteUuid } ?: throw SiteNotFoundException(syncScope.siteUuid)
        return site.toSyncScopeDto(syncScope.level)
    }
}
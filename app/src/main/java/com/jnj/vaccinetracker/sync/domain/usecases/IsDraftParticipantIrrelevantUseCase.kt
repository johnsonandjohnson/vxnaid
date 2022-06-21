package com.jnj.vaccinetracker.sync.domain.usecases

import com.jnj.vaccinetracker.common.domain.entities.DraftParticipant
import com.jnj.vaccinetracker.common.domain.entities.Site
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetSitesUseCase
import com.jnj.vaccinetracker.sync.data.models.SyncScopeLevel
import com.jnj.vaccinetracker.sync.domain.entities.SyncScope
import com.jnj.vaccinetracker.sync.domain.usecases.syncscope.BuildSyncScopeUseCase
import javax.inject.Inject

class IsDraftParticipantIrrelevantUseCase @Inject constructor(
    private val getSitesUseCase: GetSitesUseCase,
    private val buildSyncScopeUseCase: BuildSyncScopeUseCase,
) {

    private suspend fun findSites() = getSitesUseCase.getMasterData().results
    private suspend fun DraftParticipant.findSite(): Site? {
        return findSites().find { it.uuid == locationUuid }
    }

    private suspend fun SyncScope.findSite(): Site? {
        return findSites().find { it.uuid == siteUuid }
    }

    /**
     * @return **true** if [draftParticipant] is uploaded and:
     * - [draftParticipant].locationUuid is from from a different site when syncScope is SITE
     * - [draftParticipant].locationUuid is from from a different country when syncScope is COUNTRY
     * - [draftParticipant].locationUuid cannot be converted to site when syncScope is COUNTRY
     */
    suspend fun isIrrelevant(draftParticipant: DraftParticipant): Boolean {
        if (!draftParticipant.draftState.isUploaded())
            return false
        val syncScope = buildSyncScopeUseCase.buildSyncScope()

        return when (syncScope.level) {
            SyncScopeLevel.COUNTRY -> {
                val syncScopeSite = syncScope.findSite() ?: return true
                val draftParticipantSite = draftParticipant.findSite() ?: return true
                !syncScopeSite.country.equals(draftParticipantSite.country, ignoreCase = true)
            }
            SyncScopeLevel.SITE -> draftParticipant.locationUuid != syncScope.siteUuid
            SyncScopeLevel.CLUSTER -> {
                val syncScopeSite = syncScope.findSite() ?: return true
                val draftParticipantSite = draftParticipant.findSite() ?: return true
                !syncScopeSite.country.equals(draftParticipantSite.country, ignoreCase = true)
                        || !syncScopeSite.cluster.equals(draftParticipantSite.cluster, ignoreCase = true)
            }
        }
    }
}
package com.jnj.vaccinetracker.sync.domain.entities


enum class SyncPageProgress {
    /**
     * logged after we are done with a page
     */
    IDLE,

    /**
     * logged before we create a new sync request
     */
    BUILDING_SYNC_REQUEST,

    /**
     * logged when are about to download a page (participant, visit, image, biometrics_template)
     */
    DOWNLOADING_PAGE;

    fun isNotDownloading() = this != DOWNLOADING_PAGE
}
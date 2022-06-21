package com.jnj.vaccinetracker.common.data.datasources

import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.tfcporciuncula.flow.FlowSharedPreferences
import javax.inject.Inject

class SyncCompletedDateDataSource @Inject constructor(private val prefs: FlowSharedPreferences) {
    /**
     * when each [SyncEntityType] returned SYNC_OK
     */
    val syncParticipantDateCompletedPref by lazy { prefs.getLong("date_participant_data_sync_ok", 0) }
    val lastReportedSyncDatePref by lazy { prefs.getLong("date_last_reported_sync_date", 0) }
}
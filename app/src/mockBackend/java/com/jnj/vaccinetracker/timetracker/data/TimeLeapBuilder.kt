package com.jnj.vaccinetracker.timetracker.data

import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.sync.domain.usecases.GetSyncRecordCountUseCase
import com.jnj.vaccinetracker.timetracker.domain.entities.TimeLeap
import javax.inject.Inject

class TimeLeapBuilder @Inject constructor(private val syncTimeTracker: SyncTimeTracker, private val getSyncRecordCountUseCase: GetSyncRecordCountUseCase) {

    suspend fun buildTimeLeap(): TimeLeap {
        return TimeLeap(timeStamp = syncTimeTracker.getRecordedTime(), tableCounts = SyncEntityType.values().map { it to getSyncRecordCountUseCase.getCount(it) }.toMap())
    }
}
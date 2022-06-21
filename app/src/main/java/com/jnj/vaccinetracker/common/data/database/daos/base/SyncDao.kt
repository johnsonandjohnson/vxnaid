package com.jnj.vaccinetracker.common.data.database.daos.base

import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel

interface SyncDao {
    suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>
    suspend fun deleteAll()
}
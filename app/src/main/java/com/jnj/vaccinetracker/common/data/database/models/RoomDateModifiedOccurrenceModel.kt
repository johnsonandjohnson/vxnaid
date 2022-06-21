package com.jnj.vaccinetracker.common.data.database.models

import com.jnj.vaccinetracker.common.data.database.entities.base.SyncBase
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence

data class RoomDateModifiedOccurrenceModel(
    val uuid: String,
    override val dateModified: DateEntity,
) : SyncBase {
    companion object {
        fun List<RoomDateModifiedOccurrenceModel>.toDomain(): DateModifiedOccurrence? {
            val dateModified = firstOrNull()?.dateModified ?: return null
            require(all { it.dateModified == dateModified }) { "all RoomDateModifiedOccurrenceModel.dateModified in list must be identical" }
            return DateModifiedOccurrence(dateModified, map { it.uuid })
        }
    }
}
package com.jnj.vaccinetracker.common.data.database.models

import com.jnj.vaccinetracker.common.data.database.entities.base.SyncBase
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence

data class RoomDeletedParticipantModel(
    val uuid: String,
    override val dateModified: DateEntity,
    val participantId : String,
) : SyncBase



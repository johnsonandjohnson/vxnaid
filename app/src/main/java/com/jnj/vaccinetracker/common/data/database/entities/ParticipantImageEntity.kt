package com.jnj.vaccinetracker.common.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnj.vaccinetracker.common.data.database.entities.base.ImageBase
import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantSyncBase
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity

@Entity(tableName = "participant_image")
data class ParticipantImageEntity(
    @PrimaryKey
    override val participantUuid: String,
    override val imageFileName: String,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : ParticipantSyncBase, ImageBase
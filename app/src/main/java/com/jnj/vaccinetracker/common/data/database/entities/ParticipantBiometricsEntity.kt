package com.jnj.vaccinetracker.common.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnj.vaccinetracker.common.data.database.entities.base.BiometricsTemplateBase
import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantSyncBase
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity

@Entity(tableName = "participant_biometrics_template")
data class ParticipantBiometricsEntity(
    @PrimaryKey
    override val participantUuid: String,
    override val biometricsTemplateFileName: String,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : ParticipantSyncBase, BiometricsTemplateBase
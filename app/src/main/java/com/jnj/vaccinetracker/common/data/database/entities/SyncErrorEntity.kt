package com.jnj.vaccinetracker.common.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState

@Entity(tableName = "sync_error")
data class SyncErrorEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(index = true)
    val type: String,
    val metadataJson: String,
    val stackTrace: String,
    val dateCreated: DateEntity,
    @ColumnInfo(index = true)
    val syncErrorState: SyncErrorState,
)
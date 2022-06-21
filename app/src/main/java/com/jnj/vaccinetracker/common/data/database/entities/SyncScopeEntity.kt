package com.jnj.vaccinetracker.common.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.sync.data.models.SyncScopeLevel

/**
 * only one row possible -> so keep [id] to 1
 */
@Entity(tableName = "sync_scope")
data class SyncScopeEntity(
    val siteUuid: String,
    val level: SyncScopeLevel,
    val country: String?,
    val cluster: String?,
    val dateCreated: DateEntity,
    @PrimaryKey
    val id: Long = 1,
)
package com.jnj.vaccinetracker.common.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.OperatorCredentials

@Entity(tableName = "operator", indices = [Index("username", unique = true)])
class OperatorCredentialsEntity(
    @PrimaryKey
    val uuid: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val username: String,
    val display: String,
    val passwordHash: String,
    val dateCreated: DateEntity,
)

fun OperatorCredentialsEntity.toDomain() = OperatorCredentials(username = username, passwordHash = passwordHash, display = display, uuid = uuid, dateCreated = dateCreated)
fun OperatorCredentials.toPersistence() = OperatorCredentialsEntity(username = username, passwordHash = passwordHash, uuid = uuid, display = display, dateCreated = dateCreated)
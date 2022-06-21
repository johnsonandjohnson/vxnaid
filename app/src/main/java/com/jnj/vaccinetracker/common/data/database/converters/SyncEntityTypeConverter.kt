package com.jnj.vaccinetracker.common.data.database.converters

import androidx.room.TypeConverter
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType

class SyncEntityTypeConverter {

    @TypeConverter
    fun toEnum(code: String?): SyncEntityType? {
        return code?.let { SyncEntityType.fromCode(it) }
    }

    @TypeConverter
    fun toString(syncEntityType: SyncEntityType?): String? {
        return syncEntityType?.code
    }
}
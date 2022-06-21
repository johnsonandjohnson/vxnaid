package com.jnj.vaccinetracker.common.data.database.converters

import androidx.room.TypeConverter
import com.jnj.vaccinetracker.sync.data.models.SyncScopeLevel

class SyncScopeLevelConverter {
    @TypeConverter
    fun toSyncScopeLevel(code: String?): SyncScopeLevel? {
        return code?.let { SyncScopeLevel.fromCode(it) }
    }

    @TypeConverter
    fun toString(gender: SyncScopeLevel?): String? {
        return gender?.code
    }
}
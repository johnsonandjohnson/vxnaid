package com.jnj.vaccinetracker.common.data.database.converters

import androidx.room.TypeConverter
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState

class SyncErrorStateConverter {

    @TypeConverter
    fun toEnum(code: Int?): SyncErrorState? {
        return code?.let { SyncErrorState.fromCode(it) }
    }

    @TypeConverter
    fun toString(syncErrorState: SyncErrorState?): Int? {
        return syncErrorState?.code
    }
}
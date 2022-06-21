package com.jnj.vaccinetracker.common.data.database.converters

import androidx.room.TypeConverter
import com.jnj.vaccinetracker.common.domain.entities.DraftState

class DraftStateConverter {
    @TypeConverter
    fun toDraftState(code: String?): DraftState? {
        return code?.let { DraftState.fromCode(it) }
    }

    @TypeConverter
    fun toCode(draftState: DraftState?): String? {
        return draftState?.code
    }
}
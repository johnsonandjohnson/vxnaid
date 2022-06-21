package com.jnj.vaccinetracker.common.data.database.converters

import androidx.room.TypeConverter
import com.jnj.vaccinetracker.common.data.database.entities.GenderEntity

class GenderConverter {
    @TypeConverter
    fun toGender(code: String?): GenderEntity? {
        return code?.let { GenderEntity.fromCode(it) }
    }

    @TypeConverter
    fun toCode(gender: GenderEntity?): String? {
        return gender?.code
    }
}
package com.jnj.vaccinetracker.common.data.database.converters

import androidx.room.TypeConverter
import com.jnj.vaccinetracker.common.data.database.entities.BirthDateEntity
import com.jnj.vaccinetracker.common.domain.entities.BirthDate

class BirthDateConverter {
    @TypeConverter
    fun toBirthDate(date: Long?): BirthDateEntity? {
        return date?.let { BirthDate(it) }
    }

    @TypeConverter
    fun toEpochLong(birthDate: BirthDateEntity?): Long? {
        return birthDate?.time
    }
}
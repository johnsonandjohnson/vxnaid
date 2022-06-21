package com.jnj.vaccinetracker.common.data.database.converters

import androidx.room.TypeConverter
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import java.util.*

class DateConverter {
    @TypeConverter
    fun toDate(epoch: Long?): DateEntity? {
        return epoch?.let { Date(it) }
    }

    @TypeConverter
    fun toEpoch(dateTime: DateEntity?): Long? {
        return dateTime?.time
    }
}
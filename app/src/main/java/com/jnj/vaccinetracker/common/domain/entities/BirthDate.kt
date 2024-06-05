package com.jnj.vaccinetracker.common.domain.entities

import com.soywiz.klock.DateTime

data class BirthDate(val time: Long) {
    val year: Int
    val month: Int
    val day: Int

    init {
        val birthDateTime = DateTime(time)
        year = birthDateTime.yearInt
        month = birthDateTime.month.index1
        day = birthDateTime.dayOfMonth
    }

    fun toDateTime(): DateTime {
        return DateTime(year, month, day);
    }

    companion object {
        fun yearOfBirth(yearOfBirth: Int) = BirthDate(DateTime(yearOfBirth, 1, 1).unixMillisLong)
        fun yearOfBirth(yearOfBirth: String) = yearOfBirth(yearOfBirth.toInt())
    }
}

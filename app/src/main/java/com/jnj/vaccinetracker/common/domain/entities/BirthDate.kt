package com.jnj.vaccinetracker.common.domain.entities

import com.soywiz.klock.DateTime

data class BirthDate(val time: Long) {
    val year get() = DateTime(time).yearInt

    companion object {
        fun yearOfBirth(yearOfBirth: Int) = BirthDate(DateTime(yearOfBirth, 1, 1).unixMillisLong)
        fun yearOfBirth(yearOfBirth: String) = yearOfBirth(yearOfBirth.toInt())
    }
}

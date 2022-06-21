package com.jnj.vaccinetracker.common.helpers

enum class LogPriority(val number: Int) {
    PRINTLN(1), VERBOSE(2), DEBUG(3),
    INFO(4), WARN(5), ERROR(6), ASSERT(7);

    val abbreviation get() = name[0]

    val androidLog get() = number

    companion object {
        fun fromNumber(number: Int) = values().find { it.number == number } ?: PRINTLN
    }
}
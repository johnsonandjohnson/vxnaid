package com.jnj.vaccinetracker.sync.data.models

import java.util.*

data class SyncDate(val time: Long) : Comparable<SyncDate> {
    constructor(date: Date) : this(date.time)

    init {
        require(time > 0) { "time must not be zero" }
    }

    val date get() = Date(time)

    operator fun plus(t: Long): SyncDate = SyncDate(time + t)

    override fun compareTo(other: SyncDate): Int {
        return time.compareTo(other.time)
    }
}
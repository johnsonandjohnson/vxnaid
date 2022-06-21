package com.jnj.vaccinetracker.common.ui

import com.jnj.vaccinetracker.sync.data.models.SyncDate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)

fun SyncDate.format(): String = dateFormat.format(date)

operator fun Date.plus(other: Date) = Date(time + other.time)
operator fun Date.minus(other: Date) = Date(time - other.time)
operator fun Date.plus(otherTime: Long) = Date(time + otherTime)
operator fun Date.minus(otherTime: Long) = Date(time - otherTime)
val Date.dateDayStart: Date get() = TimeUnit.MILLISECONDS.toDays(time).let { TimeUnit.DAYS.toMillis(it) }.let { Date(it) }
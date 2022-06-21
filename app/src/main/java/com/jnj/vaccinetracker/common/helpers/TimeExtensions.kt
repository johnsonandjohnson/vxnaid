package com.jnj.vaccinetracker.common.helpers

val Int.milliseconds: Long get() = this.toLong()
val Int.seconds: Long get() = this.milliseconds * 1000L
val Int.minutes: Long get() = this.seconds * 60L
val Int.hours: Long get() = this.minutes * 60L
val Int.days: Long get() = this.hours * 24L
val Int.weeks: Long get() = this.days * 7L
package com.jnj.vaccinetracker.sync.p2p.data.helpers

import com.jnj.vaccinetracker.common.data.models.Constants

fun calcProgress(a: Long, b: Long): Int = (a.toDouble() / b.toDouble() * Constants.MAX_PERCENT).toInt()
fun calcProgress(a: Int, b: Int): Int = (a.toFloat() / b.toFloat() * Constants.MAX_PERCENT).toInt()
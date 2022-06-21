package com.jnj.vaccinetracker.common.data.database.typealiases

import java.util.*

typealias DateEntity = Date

fun dateNow(): DateEntity = Date()

fun yearNow(): Int = Calendar.getInstance().get(Calendar.YEAR)


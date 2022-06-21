package com.jnj.vaccinetracker.common.data.models

import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import java.util.*

typealias BirthDateDto = Date

fun BirthDate.toDto() = Date(time)
fun BirthDateDto.toDomain() = BirthDate(time)
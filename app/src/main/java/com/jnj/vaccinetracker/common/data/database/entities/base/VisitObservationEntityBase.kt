package com.jnj.vaccinetracker.common.data.database.entities.base

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue

interface VisitObservationEntityBase : VisitObservationBase {
    val value: String
    val dateTime: DateEntity
}

fun List<VisitObservationEntityBase>.toMap() = distinctBy { it.name }.map { it.name to ObservationValue(it.value, it.dateTime) }.toMap()
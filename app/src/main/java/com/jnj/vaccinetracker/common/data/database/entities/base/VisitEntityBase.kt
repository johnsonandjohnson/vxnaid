package com.jnj.vaccinetracker.common.data.database.entities.base

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity


interface VisitEntityBase : VisitBase, VisitEntityCommon {
    val visitType: String
}

interface VisitEntityCommon {
    val startDatetime: DateEntity
}
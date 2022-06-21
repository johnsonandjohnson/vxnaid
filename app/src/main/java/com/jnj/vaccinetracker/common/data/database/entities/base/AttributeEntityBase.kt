package com.jnj.vaccinetracker.common.data.database.entities.base

interface AttributeEntityBase : AttributeBase {
    val value: String
}

fun List<AttributeEntityBase>.toMap() = map { it.type to it.value }.toMap()
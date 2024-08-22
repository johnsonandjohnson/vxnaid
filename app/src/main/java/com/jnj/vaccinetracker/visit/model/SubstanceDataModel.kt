package com.jnj.vaccinetracker.visit.model

data class SubstanceDataModel(
    val conceptName: String,
    val label: String,
    val category: String,
    val routeOfAdministration: String,
    var obsDate: String? = null
)
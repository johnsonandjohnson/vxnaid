package com.jnj.vaccinetracker.visit.model

data class OtherSubstanceDataModel(
    val conceptName: String,
    val label: String,
    val category: String,
    val options: List<String>
)
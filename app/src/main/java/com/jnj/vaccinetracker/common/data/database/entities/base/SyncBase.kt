package com.jnj.vaccinetracker.common.data.database.entities.base

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity

interface SyncBase {
    val dateModified: DateEntity

    companion object {
        const val COL_DATE_MODIFIED = "dateModified"
    }
}
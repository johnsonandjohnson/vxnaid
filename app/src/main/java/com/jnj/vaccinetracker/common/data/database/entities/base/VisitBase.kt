package com.jnj.vaccinetracker.common.data.database.entities.base

/**
 * is not a base class of a visit but merely a container of data linking to a certain visit (e.g attribute, attributeId, createVisit, updateVisit,...)
 */
interface VisitBase {
    val visitUuid: String

    companion object {
        const val COL_VISIT_UUID = "visitUuid"
    }
}
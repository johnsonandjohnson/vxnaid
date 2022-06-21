package com.jnj.vaccinetracker.timetracker.domain.entities

import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType

data class TimeLeap(val tableCounts: Map<SyncEntityType, Long>, val timeStamp: Long) {

    private fun SyncEntityType.readCount() = getCount(this).toString()
    private fun Column.toValue(): String {
        return when (this) {
            Column.PARTICIPANT_COUNT -> SyncEntityType.PARTICIPANT.readCount()
            Column.IMAGE_COUNT -> SyncEntityType.IMAGE.readCount()
            Column.TEMPLATE_COUNT -> SyncEntityType.BIOMETRICS_TEMPLATE.readCount()
            Column.VISIT_COUNT -> SyncEntityType.VISIT.readCount()
            Column.TIME_STAMP_MS -> timeStamp.toString()
        }
    }

    fun toStringList(columns: List<Column> = Column.defaultList()): List<String> {
        return columns.map { it.toValue() }
    }

    fun getCount(syncEntityType: SyncEntityType): Long = tableCounts[syncEntityType] ?: 0L

    enum class Column {
        PARTICIPANT_COUNT, IMAGE_COUNT, TEMPLATE_COUNT, VISIT_COUNT, TIME_STAMP_MS;

        fun toDisplay() = name

        companion object {
            fun defaultList() = values().toList()
        }
    }
}
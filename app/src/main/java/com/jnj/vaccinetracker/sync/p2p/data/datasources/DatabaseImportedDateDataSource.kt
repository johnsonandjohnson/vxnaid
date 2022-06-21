package com.jnj.vaccinetracker.sync.p2p.data.datasources

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.tfcporciuncula.flow.FlowSharedPreferences
import javax.inject.Inject

class DatabaseImportedDateDataSource @Inject constructor(private val prefs: FlowSharedPreferences) {

    private val databaseImportedDatePref by lazy { prefs.getLong("database_imported_date", 0) }

    fun setDatabaseImportedDate(date: DateEntity) {
        databaseImportedDatePref.set(date.time)
    }

    fun getDatabaseImportedDate(): DateEntity? = databaseImportedDatePref.get().takeIf { it > 0 }?.let { DateEntity(it) }

    fun clearDatabaseImportedDate() {
        databaseImportedDatePref.delete()
    }
}
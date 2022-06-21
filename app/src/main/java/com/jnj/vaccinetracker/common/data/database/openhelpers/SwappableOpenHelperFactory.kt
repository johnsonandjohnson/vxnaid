package com.jnj.vaccinetracker.common.data.database.openhelpers

import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.jnj.vaccinetracker.common.data.database.DbInfo

class SwappableOpenHelperFactory(private val dbInfo: DbInfo) :
    SupportSQLiteOpenHelper.Factory {
    var openHelper: SwappableOpenHelper? = null

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return SwappableOpenHelper(configuration, dbInfo).also {
            openHelper = it
        }
    }
}
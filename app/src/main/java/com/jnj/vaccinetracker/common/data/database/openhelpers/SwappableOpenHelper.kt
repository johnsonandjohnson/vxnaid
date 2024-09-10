package com.jnj.vaccinetracker.common.data.database.openhelpers

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.jnj.vaccinetracker.common.data.database.DbInfo
import com.jnj.vaccinetracker.common.data.database.helpers.createSupportFactory
import com.jnj.vaccinetracker.common.data.database.helpers.rethrowErrorsAsNative
import com.jnj.vaccinetracker.common.data.database.openhelpers.helpers.RemoveSqlCipherExceptionSupportSqliteDatabase.Companion.withoutSqlCipherException

class SwappableOpenHelper(
    private val configuration: SupportSQLiteOpenHelper.Configuration,
    private val dbInfo: DbInfo,
) : SupportSQLiteOpenHelper {

    private var openHelperDelegate: SupportSQLiteOpenHelper = createOpenHelper()

    private fun createOpenHelper() = createSupportFactory(dbInfo.passphraseProvider()).create(configuration)

    fun recreateOpenHelper() {
        openHelperDelegate = createOpenHelper()
    }

    override fun close() {
        openHelperDelegate.close()
    }

    override val databaseName: String?
        get() = openHelperDelegate.databaseName

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        openHelperDelegate.setWriteAheadLoggingEnabled(enabled)
    }

    override val writableDatabase: SupportSQLiteDatabase
        get() = rethrowErrorsAsNative {
            openHelperDelegate.writableDatabase.withoutSqlCipherException()
        }

    override val readableDatabase: SupportSQLiteDatabase
        get() = rethrowErrorsAsNative {
            openHelperDelegate.readableDatabase.withoutSqlCipherException()
        }
}

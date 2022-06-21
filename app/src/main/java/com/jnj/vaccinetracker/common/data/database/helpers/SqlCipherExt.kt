package com.jnj.vaccinetracker.common.data.database.helpers

import android.database.SQLException
import android.database.sqlite.SQLiteException
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.SQLException as CipherSQLException
import net.sqlcipher.database.SQLiteException as CipherSQLiteException


/**
 * InvalidationTracker catches the native exception
 */
fun CipherSQLiteException.toAndroidSqliteException(): SQLiteException = SQLiteException(message, cause)
fun CipherSQLException.toAndroidSqlException(): SQLException = SQLException(message, cause)

fun createSupportFactory(passphrase: String) =
    SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))


inline fun <reified T> rethrowErrorsAsNative(block: () -> T): T {
    return try {
        block()
    } catch (ex: CipherSQLException) {
        throw ex.toAndroidSqlException()
    } catch (ex: CipherSQLiteException) {
        throw ex.toAndroidSqliteException()
    }
}
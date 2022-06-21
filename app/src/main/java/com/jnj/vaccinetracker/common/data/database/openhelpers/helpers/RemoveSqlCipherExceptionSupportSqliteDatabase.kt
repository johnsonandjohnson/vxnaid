package com.jnj.vaccinetracker.common.data.database.openhelpers.helpers

import android.content.ContentValues
import android.database.Cursor
import android.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import com.jnj.vaccinetracker.common.data.database.helpers.rethrowErrorsAsNative

/**
 * InvalidationTracker catches a native SqliteException but not the SQLCipher variant causing an uncaught exception crashing the app.
 * This implementation resolves this issue.
 * Stack trace that can get thrown by invalidation tracker:
 * ```
 *  +net.sqlcipher.database.SQLiteException: no such table: room_table_modification_log: , while compiling: SELECT * FROM room_table_modification_log WHERE invalidated = 1;
 * ```
 */
class RemoveSqlCipherExceptionSupportSqliteDatabase(private val db: SupportSQLiteDatabase) : SupportSQLiteDatabase by db {

    override fun compileStatement(sql: String?): SupportSQLiteStatement = rethrowErrorsAsNative {
        db.compileStatement(sql)
    }

    override fun query(query: String?): Cursor = rethrowErrorsAsNative {
        db.query(query)
    }

    override fun query(query: String?, bindArgs: Array<out Any>?): Cursor = rethrowErrorsAsNative {
        db.query(query, bindArgs)
    }

    override fun query(query: SupportSQLiteQuery?): Cursor = rethrowErrorsAsNative {
        db.query(query)
    }

    override fun query(query: SupportSQLiteQuery?, cancellationSignal: CancellationSignal?): Cursor = rethrowErrorsAsNative {
        db.query(query)
    }

    override fun insert(table: String?, conflictAlgorithm: Int, values: ContentValues?): Long = rethrowErrorsAsNative {
        return db.insert(table, conflictAlgorithm, values)
    }

    override fun delete(table: String?, whereClause: String?, whereArgs: Array<out Any>?): Int = rethrowErrorsAsNative {
        return db.delete(table, whereClause, whereArgs)
    }

    override fun update(table: String?, conflictAlgorithm: Int, values: ContentValues?, whereClause: String?, whereArgs: Array<out Any>?): Int = rethrowErrorsAsNative {
        return db.update(table, conflictAlgorithm, values, whereClause, whereArgs)
    }

    override fun execSQL(sql: String?) {
        rethrowErrorsAsNative {
            db.execSQL(sql)
        }

    }

    override fun execSQL(sql: String?, bindArgs: Array<out Any>?) {
        rethrowErrorsAsNative {
            db.execSQL(sql, bindArgs)
        }
    }

    override fun close() {
        rethrowErrorsAsNative {
            db.close()
        }
    }

    companion object {
        fun SupportSQLiteDatabase.withoutSqlCipherException() = RemoveSqlCipherExceptionSupportSqliteDatabase(this)
    }
}
package com.jnj.vaccinetracker.common.data.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteException
import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.converters.*
import com.jnj.vaccinetracker.common.data.database.daos.*
import com.jnj.vaccinetracker.common.data.database.daos.base.count
import com.jnj.vaccinetracker.common.data.database.daos.draft.*
import com.jnj.vaccinetracker.common.data.database.entities.*
import com.jnj.vaccinetracker.common.data.database.entities.draft.*
import com.jnj.vaccinetracker.common.data.database.openhelpers.SwappableOpenHelperFactory
import com.jnj.vaccinetracker.common.data.database.openhelpers.helpers.onAutoCloseCallbackReflection
import com.jnj.vaccinetracker.common.data.repositories.EncryptionKeyRepository
import com.jnj.vaccinetracker.common.exceptions.DeleteDatabaseRequiredException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.config.appSettings
import com.jnj.vaccinetracker.sync.p2p.common.util.DatabaseFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

object ParticipantRoomDatabaseConfig {
    const val CURRENT_VERSION = 12
    const val FILE_NAME = "participants.db"
}

@Database(
    entities = [
        ParticipantEntity::class,
        ParticipantBiometricsEntity::class,
        ParticipantImageEntity::class,
        ParticipantAddressEntity::class,
        ParticipantAttributeEntity::class,
        VisitEntity::class,
        VisitAttributeEntity::class,
        VisitObservationEntity::class,
        FailedVisitSyncRecordDownloadEntity::class,
        FailedParticipantSyncRecordDownloadEntity::class,
        FailedImageSyncRecordDownloadEntity::class,
        FailedBiometricsTemplateSyncRecordDownloadEntity::class,
        DeletedVisitEntity::class,
        DeletedParticipantEntity::class,
        DeletedParticipantImageEntity::class,
        DeletedParticipantBiometricsTemplateEntity::class,
        //DRAFT ENTITIES

        DraftParticipantEntity::class,
        DraftParticipantAddressEntity::class,
        DraftParticipantAttributeEntity::class,
        DraftParticipantBiometricsEntity::class,
        DraftParticipantImageEntity::class,
        DraftVisitEntity::class,
        DraftVisitAttributeEntity::class,
        DraftVisitEncounterEntity::class,
        DraftVisitEncounterObservationEntity::class,
        DraftVisitEncounterAttributeEntity::class,
        //MISC
        SyncScopeEntity::class,
        OperatorCredentialsEntity::class,
        SyncErrorEntity::class,
    ],
    version = ParticipantRoomDatabaseConfig.CURRENT_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
    ]
)
@TypeConverters(
    DateConverter::class,
    GenderConverter::class,
    DraftStateConverter::class,
    BirthDateConverter::class,
    SyncScopeLevelConverter::class,
    SyncEntityTypeConverter::class,
    SyncErrorStateConverter::class
)
abstract class ParticipantRoomDatabase : RoomDatabase() {
    abstract fun participantDao(): ParticipantDao
    abstract fun participantBiometricsTemplateDao(): ParticipantBiometricsTemplateDao
    abstract fun participantImageDao(): ParticipantImageDao
    abstract fun participantAddressDao(): ParticipantAddressDao
    abstract fun participantAttributeDao(): ParticipantAttributeDao
    abstract fun visitDao(): VisitDao
    abstract fun visitAttributeDao(): VisitAttributeDao
    abstract fun visitObservationDao(): VisitObservationDao
    abstract fun failedVisitDownloadDao(): FailedVisitSyncRecordDownloadDao
    abstract fun failedParticipantDownloadDao(): FailedParticipantSyncRecordDownloadDao
    abstract fun failedImageDownloadDao(): FailedImageSyncRecordDownloadDao
    abstract fun failedBiometricsTemplateDownloadDao(): FailedBiometricsTemplateSyncRecordDownloadDao
    abstract fun deletedVisitDownloadDao(): DeletedVisitDao
    abstract fun deletedParticipantDownloadDao(): DeletedParticipantDao
    abstract fun deletedImageDownloadDao(): DeletedImageDao
    abstract fun deletedBiometricsTemplateDownloadDao(): DeletedBiometricsTemplateDao
    //DRAFT DAOS

    abstract fun draftParticipantDao(): DraftParticipantDao
    abstract fun draftParticipantAddressDao(): DraftParticipantAddressDao
    abstract fun draftParticipantAttributeDao(): DraftParticipantAttributeDao
    abstract fun draftParticipantBiometricsTemplateDao(): DraftParticipantBiometricsTemplateDao
    abstract fun draftParticipantImageDao(): DraftParticipantImageDao
    abstract fun draftVisitDao(): DraftVisitDao
    abstract fun draftVisitAttributeDao(): DraftVisitAttributeDao
    abstract fun draftVisitEncounterDao(): DraftVisitEncounterDao
    abstract fun draftVisitEncounterObservationDao(): DraftVisitEncounterObservationDao
    abstract fun draftVisitEncounterAttributeDao(): DraftVisitEncounterAttributeDao

    //MISC
    abstract fun syncScopeDao(): SyncScopeDao
    abstract fun operatorCredentialsDao(): OperatorCredentialsDao
    abstract fun syncErrorDao(): SyncErrorDao


    private var factory: SwappableOpenHelperFactory? = null

    private val mutex = Mutex()

    /**
     * This will reload the passphrase from the [EncryptionKeyRepository]
     * and create a WAL checkpoint so all data is flushed to the main db file.
     */
    suspend fun reopen() {
        logInfo("reopen")
        closeShorty { }
    }

    suspend fun closeShorty(coroutineContext: CoroutineContext = Dispatchers.IO, doWhileClosed: suspend () -> Unit) {
        closeSilently(coroutineContext, doWhileClosed)
    }

    private suspend fun closeSilentlyImpl(doWhileClosed: suspend () -> Unit) {
        logInfo("closeSilently")
        val instance = factory?.openHelper
        if (instance != null) {
            invalidationTracker.onAutoCloseCallbackReflection()
            try {
                instance.close()
            } catch (ex: SQLiteException) {
                if (ex.message?.contains("unable to close due to unfinalized statements or unfinished backups") == true) {
                    logError("couldn't close due to unfinalized statements or unfinished backups, trying again in 3 seconds")
                    delay(3000)
                    return closeSilentlyImpl(doWhileClosed)
                } else {
                    logError("SQLiteException during close database", ex)
                    throw ex
                }
            }
            try {
                doWhileClosed()
            } finally {
                // recreate the open helper so the database can be reopened
                // (passphrase is emptied in SQLCypher support factory)
                instance.recreateOpenHelper()
            }
        } else {
            logError("closeSilently: database instance is null")
        }
    }

    /**
     * When database is closed a checkout will be performed so the -wal en -shm files are cleared, meaning we have to transfer only one file.
     * In standard [close] you'll get following exception:
     *  ```
     *  java.lang.IllegalStateException: Cannot perform this operation because the connection pool has been closed.
     *  ```
     *  To solve this add an setAutoCloseTimeout and keep reference of original SupportSQLiteOpenHelper.
     *  Because setAutoCloseTimeout will create an AutoCloseSqliteOpenHelper which closes db after some time of unuse.
     *  If we call close on that instance, they'll mark it as manually closed.
     *  However if we call close on our own open helper, they'll assume it was closed automatically and will recreate it on next query
     */
    private suspend fun closeSilently(coroutineContext: CoroutineContext, doWhileClosed: suspend () -> Unit) {
        withContext(coroutineContext) {
            mutex.withLock {
                closeSilentlyImpl(doWhileClosed)
            }
        }
    }

    class Factory @Inject constructor(
        private val context: Context,
        private val encryptionKeyRepository:
        EncryptionKeyRepository,
        private val dispatchers: AppCoroutineDispatchers,
        private val databaseFolder: DatabaseFolder,
    ) {

        private fun defaultPassphraseProvider(): PassphraseProvider {
            return {
                val passphrase = runBlocking { encryptionKeyRepository.getOrGenerateDatabasePassphrase() }
                if (appSettings.isUatOrLess) {
                    logInfo("passphrase: $passphrase")
                }
                passphrase
            }
        }

        suspend fun createDatabaseWithDefaultPassphrase(name: String = ParticipantRoomDatabaseConfig.FILE_NAME, deleteDatabaseIfCorrupt: Boolean): ParticipantRoomDatabase =
            withContext(dispatchers.io) {
                try {
                    val database = createDatabase(name, defaultPassphraseProvider())
                    if (database.isCorrupt()) {
                        onDatabaseCorrupt(name, database, deleteDatabaseIfCorrupt)
                    } else {
                        database
                    }
                } catch (ex: SQLException) {
                    if (ex.isCorruptDatabase()) {
                        onDatabaseCorrupt(name, database = null, deleteDatabaseIfCorrupt)
                    } else {
                        throw ex
                    }
                }
            }

        private suspend fun onDatabaseCorrupt(name: String, database: ParticipantRoomDatabase?, deleteDatabaseIfCorrupt: Boolean): ParticipantRoomDatabase {
            logWarn("database is corrupt")
            return if (deleteDatabaseIfCorrupt) {
                logError("have to delete database because it's corrupt")
                kotlin.runCatching { database?.close() }
                val success = databaseFolder.deleteDatabase(name)
                if (success) {
                    createDatabaseWithDefaultPassphrase(name, false)
                } else {
                    throw DeleteDatabaseRequiredException(dbName = name)
                }
            } else {
                logInfo("delete database required")
                throw DeleteDatabaseRequiredException(dbName = name)
            }
        }

        @SuppressLint("UnsafeOptInUsageError")
        fun createDatabase(
            name: String,
            passphraseProvider: PassphraseProvider,
        ): ParticipantRoomDatabase {
            val dbInfo = DbInfo(name, passphraseProvider)
            val swappableFactory = SwappableOpenHelperFactory(dbInfo)
            return Room.databaseBuilder(
                context,
                ParticipantRoomDatabase::class.java,
                name,
            ).setAutoCloseTimeout(1000, TimeUnit.DAYS)
                .openHelperFactory(swappableFactory)
                .build().also {
                    it.factory = swappableFactory
                }
        }

        private fun Exception.isCorruptDatabase() = message?.contains("file is not a database") == true


        private suspend fun ParticipantRoomDatabase.isCorrupt(): Boolean {
            return try {
                testQuery()
                false
            } catch (ex: SQLiteException) {
                logError("testQuery failed", ex)
                if (ex.isCorruptDatabase()) {
                    true
                } else {
                    throw ex
                }
            }
        }

        private suspend fun ParticipantRoomDatabase.testQuery() {
            participantDao().count()
        }
    }
}

typealias PassphraseProvider = () -> String

data class DbInfo(val name: String, val passphraseProvider: PassphraseProvider)
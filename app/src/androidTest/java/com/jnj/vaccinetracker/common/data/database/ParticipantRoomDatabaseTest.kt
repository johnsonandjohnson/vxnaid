package com.jnj.vaccinetracker.common.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jnj.vaccinetracker.VaccineTrackerApplication
import com.jnj.vaccinetracker.common.helpers.Logger
import com.jnj.vaccinetracker.di.DaggerTestDaggerComponent
import com.jnj.vaccinetracker.sync.p2p.common.util.DatabaseFolder
import de.codecentric.androidtestktx.common.appContext
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject


@RunWith(AndroidJUnit4::class)
class ParticipantRoomDatabaseTest {
    private val app = appContext.applicationContext as VaccineTrackerApplication

    @Inject
    lateinit var databaseFactory: ParticipantRoomDatabase.Factory

    @Inject
    lateinit var databaseFolder: DatabaseFolder

    lateinit var database: ParticipantRoomDatabase

    companion object {
        private const val TEST_DB_NAME = "test-database.db"
    }

    @Before
    fun setUp() {
        val comp = DaggerTestDaggerComponent.builder().application(app).build()
        comp.inject(this)
        database = createTestDatabase()
        Logger.ENABLED = true
    }

    private fun createTestDatabase(): ParticipantRoomDatabase = runBlocking {
        databaseFactory.createDatabaseWithDefaultPassphrase(name = TEST_DB_NAME, false)
    }

    private suspend fun goIntoTransactionForAWhile() {
        val duration = 10_000L
        println("goIntoTransactionForAWhile: $duration")
        database.beginTransaction()
        try {
            println("withTransaction")
            delay(duration)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        println("database transaction finished")
    }

    private fun CoroutineScope.runGoIntoTransactionForAWhileJob() {
        launch {
            try {
                goIntoTransactionForAWhile()
            } catch (ex: Exception) {
                yield()
                println("transaction closed")
                ex.printStackTrace()
            }
        }
    }

    @Test
    fun testCloseSilentlyWhenInTransaction() {
        // for this test, observe the logcat
        runBlocking {
            coroutineScope {
                runGoIntoTransactionForAWhileJob()
                delay(1000)
                launch {
                    println("closing database shortly")
                    // expected: the internal close function will block until all transactions are completed
                    database.closeShorty {
                        println("database closed for 5 seconds")
                        // expected: transaction will end immediately with exception because the database is not openable during closeShortly
                        runGoIntoTransactionForAWhileJob()
                        delay(5000)
                    }
                    println("database reopened")
                }
            }


        }
    }

    @After
    fun tearDown() {
        databaseFolder.deleteDatabase(TEST_DB_NAME)
    }
}
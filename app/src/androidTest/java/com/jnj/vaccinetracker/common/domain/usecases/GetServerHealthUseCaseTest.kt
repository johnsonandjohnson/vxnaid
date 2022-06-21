package com.jnj.vaccinetracker.common.domain.usecases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jnj.vaccinetracker.VaccineTrackerApplication
import com.jnj.vaccinetracker.common.helpers.Logger
import com.jnj.vaccinetracker.di.DaggerTestDaggerComponent
import com.jnj.vaccinetracker.sync.data.models.SyncUserCredentials
import de.codecentric.androidtestktx.common.appContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class GetServerHealthUseCaseTest {
    private val app = appContext.applicationContext as VaccineTrackerApplication

    @Inject
    lateinit var getServerHealthUseCase: GetServerHealthUseCase

    @Inject
    lateinit var syncAdminLoginUseCase: SyncAdminLoginUseCase


    @Before
    fun setUp() {
        val comp = DaggerTestDaggerComponent.builder().application(app).build()
        comp.inject(this)
    }

    @Test
    fun test() {
        Logger.ENABLED = true
        runBlocking {
            syncAdminLoginUseCase.login(SyncUserCredentials("syncadmin", "Admin123"))
            val result = getServerHealthUseCase.getHealth().getOrThrow()
            assertEquals(200, result)
        }

    }
}
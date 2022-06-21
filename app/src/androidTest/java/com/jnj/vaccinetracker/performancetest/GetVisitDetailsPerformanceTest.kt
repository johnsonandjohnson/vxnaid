package com.jnj.vaccinetracker.performancetest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jnj.vaccinetracker.VaccineTrackerApplication
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.domain.usecases.GetParticipantVisitDetailsUseCase
import com.jnj.vaccinetracker.di.DaggerTestDaggerComponent
import de.codecentric.androidtestktx.common.appContext
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@LargeTest
class GetVisitDetailsPerformanceTest {

    private val app = appContext.applicationContext as VaccineTrackerApplication

    @Inject
    lateinit var getParticipantVisitDetailsUseCase: GetParticipantVisitDetailsUseCase

    @Inject
    lateinit var participantRepository: ParticipantRepository
    lateinit var participantUuid: String

    /**
     * fetch visits related to this participantId
     */
    private val participantId: String = "pa-50000"

    @Before
    fun setUp() {
        val comp = DaggerTestDaggerComponent.builder().application(app).build()
        comp.inject(this)
        runBlocking {
            val uuid = participantRepository.findByParticipantId(participantId)?.participantUuid ?: error("participantId $participantId not found")
            participantUuid = uuid
        }
    }

    @Test
    fun testGetVisitDetailsPerformance() {
        runBlocking {
            getParticipantVisitDetailsUseCase.getParticipantVisitDetails(participantUuid)
        }

    }
}
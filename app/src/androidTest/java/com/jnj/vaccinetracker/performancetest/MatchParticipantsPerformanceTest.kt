package com.jnj.vaccinetracker.performancetest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jnj.vaccinetracker.VaccineTrackerApplication
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.common.domain.entities.ParticipantIdentificationCriteria
import com.jnj.vaccinetracker.common.domain.usecases.MatchParticipantsUseCase
import com.jnj.vaccinetracker.di.DaggerTestDaggerComponent
import com.jnj.vaccinetracker.readResource
import de.codecentric.androidtestktx.common.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@LargeTest
class MatchParticipantsPerformanceTest {
    private val app = appContext.applicationContext as VaccineTrackerApplication

    @Inject
    lateinit var matchParticipantsUseCase: MatchParticipantsUseCase

    private val participantId: String = "pa-50000"
    private val phone: String = "32123456789"
    private val templateFileName = "003" // in resources folder
    private val template: BiometricsTemplateBytes = runBlocking(Dispatchers.IO) {
        readResource(templateFileName).buffered().use { it.readBytes() }.let { BiometricsTemplateBytes(it) }
    }

    @Before
    fun setUp() {
        val comp = DaggerTestDaggerComponent.builder().application(app).build()
        comp.inject(this)
    }

    private fun doMatching(criteria: ParticipantIdentificationCriteria) = runBlocking {
        matchParticipantsUseCase.matchParticipants(criteria)
    }

    @Test
    fun testParticipantIdAndTemplate() {
        doMatching(ParticipantIdentificationCriteria(participantId = participantId, biometricsTemplate = template, phone = null))
    }

    @Test
    fun testPhoneAndTemplate() {
        doMatching(ParticipantIdentificationCriteria(participantId = null, biometricsTemplate = template, phone = phone))
    }


    @Test
    fun testPhoneAndParticipantIdAndTemplate() {
        doMatching(ParticipantIdentificationCriteria(participantId = participantId, biometricsTemplate = template, phone = phone))
    }


    @Test
    fun testTemplate() {
        doMatching(ParticipantIdentificationCriteria(participantId = null, biometricsTemplate = template, phone = null))
    }

    @Test
    fun testPhone() {
        doMatching(ParticipantIdentificationCriteria(participantId = null, biometricsTemplate = null, phone = phone))
    }

    @Test
    fun testParticipantId() {
        doMatching(ParticipantIdentificationCriteria(participantId = participantId, biometricsTemplate = null, phone = null))
    }

    @Test
    fun testPhoneAndParticipantId() {
        doMatching(ParticipantIdentificationCriteria(participantId = participantId, biometricsTemplate = null, phone = phone))
    }


}
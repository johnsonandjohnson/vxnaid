package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetVaccineScheduleUseCase
import com.jnj.vaccinetracker.common.helpers.days
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.common.ui.minus
import com.jnj.vaccinetracker.common.ui.plus
import com.jnj.vaccinetracker.sync.data.models.ScheduledVisit
import com.jnj.vaccinetracker.sync.data.models.VaccineRegimen
import com.jnj.vaccinetracker.sync.data.models.VaccineSchedule
import com.jnj.vaccinetracker.sync.data.models.VisitType
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit.Companion.toUpcomingVisit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import java.util.*

class GetUpcomingVisitUseCaseTest : FunSpec({
    val getVaccineScheduleUseCase: GetVaccineScheduleUseCase = mockk()
    val getParticipantVisitDetailsUseCase: GetParticipantVisitDetailsUseCase = mockk()
    val getParticipantRegimenUseCase: GetParticipantRegimenUseCase = mockk()
    val getUpcomingVisitUseCase = GetUpcomingVisitUseCase(getVaccineScheduleUseCase = getVaccineScheduleUseCase, getParticipantVisitDetailsUseCase, getParticipantRegimenUseCase)
    val participantUuid = uuid()
    val regimen = "Covid 1D Vaccine"
    lateinit var vaccineSchedule: VaccineSchedule


    lateinit var visits: List<VisitDetail>
    coEvery { getParticipantRegimenUseCase.getParticipantRegimen(participantUuid) } returns regimen
    coEvery { getVaccineScheduleUseCase.getMasterData() } coAnswers {
        vaccineSchedule
    }
    coEvery { getParticipantVisitDetailsUseCase.getParticipantVisitDetails(participantUuid) } coAnswers {
        visits
    }

    suspend fun getUpcomingVisit() = getUpcomingVisitUseCase.getUpcomingVisit(participantUuid, dateNow())

    fun visitDetail(visitType: VisitType, visitDate: Date, visitStatus: String, lowWindow: Int? = null, upWindow: Int? = null, doseNumber: Int? = 1) = VisitDetail(
        uuid(),
        visitType.key,
        visitDate,
        attributes = listOf(
            Constants.ATTRIBUTE_VISIT_STATUS to visitStatus,
            Constants.ATTRIBUTE_VISIT_DAYS_BEFORE to lowWindow?.toString(),
            Constants.ATTRIBUTE_VISIT_DAYS_AFTER to upWindow?.toString(),
            Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to doseNumber?.toString(),
        ).mapNotNull { (k, v) -> if (v != null) k to v else null }.toMap(),
        observations = emptyMap()
    )

    fun otherVisit(visitDate: Date) = visitDetail(VisitType.OTHER, visitDate, visitStatus = Constants.VISIT_STATUS_OCCURRED)

    fun createVaccineSchedule(scheduledVisits: List<ScheduledVisit>) =
        listOf(
            VaccineRegimen("fake", 0, emptyList()),
            VaccineRegimen(regimen, scheduledVisits.count { it.visitType == VisitType.DOSING }, scheduledVisits),
        )

    fun scheduledDosingVisit(
        doseNumber: Int, windowBefore: Int = 1,
        daysFromLastDose: Int = 7,
        windowAfter: Int = 1
    ) = ScheduledVisit(
        doseNumber = doseNumber,
        nameOfDose = VisitType.DOSING.key,

        windowBefore = windowBefore,
        daysFromLastDose = daysFromLastDose,
        windowAfter = windowAfter
    )

    fun scheduledFollowUpVisit(daysFromLastDose: Int) = ScheduledVisit(
        doseNumber = 0,
        nameOfDose = VisitType.IN_PERSON_FOLLOW_UP.key,

        windowBefore = 0,
        daysFromLastDose = daysFromLastDose,
        windowAfter = 0
    )
    context("given vaccine schedule is not empty") {
        test("and participant visits is empty then return null") {
            // Arrange
            val dosingVisit = visitDetail(VisitType.DOSING, dateNow(), Constants.VISIT_STATUS_OCCURRED)
            visits = listOf(dosingVisit)
            vaccineSchedule = createVaccineSchedule(
                listOf(
                    scheduledDosingVisit(1)
                )
            )
            // Act
            val upcomingVisit = getUpcomingVisit()
            // Assert
            upcomingVisit.shouldBeNull()
        }
        context("and last occurred participant visit is dosing type") {
            data class TestValue(val visitType: VisitType, val expectNull: Boolean)

            VisitType.values().map {
                TestValue(it, it !in Constants.SUPPORTED_UPCOMING_VISIT_TYPES)
            }.forEach { (visitType, expectNull) ->
                test("and vaccine schedule has $visitType visit afterwards then return ${if (expectNull) "null" else "that"}") {
                    // Arrange
                    val dosingVisit = visitDetail(VisitType.DOSING, dateNow(), Constants.VISIT_STATUS_OCCURRED)
                    visits = listOf(dosingVisit)
                    val followUpVisit = ScheduledVisit(
                        doseNumber = 2,
                        nameOfDose = visitType.key,
                        windowBefore = 0,
                        daysFromLastDose = 7,
                        windowAfter = 0
                    )

                    vaccineSchedule = createVaccineSchedule(
                        listOf(
                            scheduledDosingVisit(1),
                            followUpVisit
                        )
                    )
                    // Act
                    val upcomingVisit = getUpcomingVisit()
                    // Assert
                    if (expectNull)
                        upcomingVisit.shouldBeNull()
                    else
                        upcomingVisit shouldBe followUpVisit.toUpcomingVisit(dosingVisit)
                }
            }
        }

        test("given order of vaccine schedule is not chronological return the chronological next visit") {
            // Arrange
            val dosingVisit = visitDetail(VisitType.DOSING, dateNow(), Constants.VISIT_STATUS_OCCURRED)
            visits = listOf(dosingVisit)
            val nextDosingVisit = scheduledDosingVisit(2, daysFromLastDose = 2, windowBefore = 1, windowAfter = 1)
            vaccineSchedule = createVaccineSchedule(
                listOf(
                    scheduledDosingVisit(1),
                    scheduledFollowUpVisit(daysFromLastDose = 3),
                    nextDosingVisit
                )
            )
            // Act
            val upcomingVisit = getUpcomingVisit()
            // Assert
            upcomingVisit shouldBe nextDosingVisit.toUpcomingVisit(dosingVisit)
        }
    }


    context("given vaccine schedule is empty") {
        // Arrange
        vaccineSchedule = createVaccineSchedule(emptyList())

        test("and participant visits has only an occurred dosing visit then return null") {
            // Arrange
            val dosingVisit = visitDetail(VisitType.DOSING, dateNow(), Constants.VISIT_STATUS_OCCURRED)
            visits = listOf(dosingVisit)

            // Act
            val upcomingVisit = getUpcomingVisit()
            // Assert
            upcomingVisit.shouldBeNull()
        }

        test("and participant visits has only a missed dosing visit then return null") {
            // Arrange
            val dosingVisit = visitDetail(VisitType.DOSING, dateNow(), Constants.VISIT_STATUS_MISSED)
            visits = listOf(dosingVisit)
            // Act
            val upcomingVisit = getUpcomingVisit()
            // Assert
            upcomingVisit.shouldBeNull()
        }
        test("and participant visits has a scheduled dosing visit then return it") {
            // Arrange
            val dosingVisit = visitDetail(VisitType.DOSING, dateNow() + 1.days, Constants.VISIT_STATUS_SCHEDULED)
            visits = listOf(dosingVisit)
            // Act
            val upcomingVisit = getUpcomingVisit()
            // Assert
            upcomingVisit shouldBe dosingVisit.toUpcomingVisit()
        }

        test("and participant visits has one occurred OTHER visit after one scheduled dosing visit then return the dosing visit") {
            // Arrange
            val dosingVisit = visitDetail(VisitType.DOSING, dateNow() + 1.days, Constants.VISIT_STATUS_SCHEDULED)
            visits = listOf(dosingVisit, otherVisit(dateNow() + 2.days))
            // Act
            val upcomingVisit = getUpcomingVisit()
            // Assert
            upcomingVisit shouldBe dosingVisit.toUpcomingVisit()
        }


    }

    context("given last occurred participant visit is not dosing type") {
        val dosingVisit = visitDetail(VisitType.DOSING, dateNow() - 5.days, Constants.VISIT_STATUS_OCCURRED)
        val otherVisit = otherVisit(dateNow())
        context("and participant has occurred dosing visit before it") {
            visits = listOf(dosingVisit, otherVisit)

            test("and next dosing visit available in participant visits but vaccine schedule is empty then return null") {
                // Arrange
                vaccineSchedule = createVaccineSchedule(emptyList())
                val dosingVisitNext = visitDetail(VisitType.DOSING, dateNow() + 7.days, Constants.VISIT_STATUS_SCHEDULED, doseNumber = 2)
                visits = visits + dosingVisitNext
                // Act
                val upcomingVisit = getUpcomingVisit()
                // Assert
                upcomingVisit.shouldBeNull()
            }
            test("and next dosing visit available without dosing number in participant visits but vaccine schedule is empty then return null") {
                // Arrange
                vaccineSchedule = createVaccineSchedule(emptyList())
                val dosingVisitNext = visitDetail(VisitType.DOSING, dateNow() + 7.days, Constants.VISIT_STATUS_SCHEDULED, doseNumber = null)
                visits = visits + dosingVisitNext
                // Act
                val upcomingVisit = getUpcomingVisit()
                // Assert
                upcomingVisit.shouldBeNull()
            }
            context("and next dosing visit not available in participant visits") {
                test("and next dosing visit with next dosing number is available in scheduled visits then return that visit") {
                    // Arrange
                    val dosingVisitNext = scheduledDosingVisit(
                        2,
                        windowBefore = 1,
                        daysFromLastDose = 7,
                        windowAfter = 1
                    )
                    vaccineSchedule = createVaccineSchedule(listOf(scheduledDosingVisit(1), dosingVisitNext))

                    // Act
                    val upcomingVisit = getUpcomingVisit()
                    // Assert
                    upcomingVisit shouldBe dosingVisitNext.toUpcomingVisit(dosingVisit)
                }
                context("and a future follow up visit available in scheduled visits") {
                    // Arrange
                    val nextFollowUpVisit = scheduledFollowUpVisit(
                        daysFromLastDose = 10,
                    )
                    vaccineSchedule = createVaccineSchedule(listOf(scheduledDosingVisit(1), nextFollowUpVisit))
                    test("and occurred dose number 1 dosing visit is available then return follow up visit based on that dosing visit") {
                        // Act
                        val upcomingVisit = getUpcomingVisit()
                        // Assert
                        upcomingVisit shouldBe nextFollowUpVisit.toUpcomingVisit(dosingVisit)
                    }
                    test("and occurred dose number 1 dosing visit is not available then return null") {
                        val dosingVisit2 = dosingVisit.copy(attributes = dosingVisit.attributes + (Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to "2"))
                        visits = listOf(dosingVisit2, otherVisit)
                        // Act
                        val upcomingVisit = getUpcomingVisit()
                        // Assert
                        upcomingVisit.shouldBeNull()
                    }
                }
                test("and only past follow up visit available then return null") {
                    // Arrange
                    vaccineSchedule = createVaccineSchedule(
                        listOf(
                            scheduledDosingVisit(
                                doseNumber = 1,
                            ),
                            scheduledFollowUpVisit(
                                daysFromLastDose = 1,
                            ),
                        )
                    )
                    // Act
                    val upcomingVisit = getUpcomingVisit()
                    // Assert
                    upcomingVisit.shouldBeNull()
                }
                listOf(VisitType.IN_PERSON_FOLLOW_UP, VisitType.OTHER).forEach { visitType ->
                    test("and four scheduled $visitType visits available and only fourth one has dosing window after occurred OTHER participant visit then return that fourth one") {
                        // Arrange
                        val fourthFollowUpVisit = ScheduledVisit(
                            doseNumber = 4,
                            nameOfDose = visitType.key,

                            windowBefore = 1,
                            daysFromLastDose = 10,
                            windowAfter = 1
                        )
                        vaccineSchedule = createVaccineSchedule(
                            listOf(
                                scheduledDosingVisit(
                                    doseNumber = 1,
                                ),
                                ScheduledVisit(
                                    doseNumber = 1,
                                    nameOfDose = visitType.key,

                                    windowBefore = 1,
                                    daysFromLastDose = 1,
                                    windowAfter = 1
                                ),
                                ScheduledVisit(
                                    doseNumber = 2,
                                    nameOfDose = visitType.key,

                                    windowBefore = 1,
                                    daysFromLastDose = 2,
                                    windowAfter = 1
                                ),
                                ScheduledVisit(
                                    doseNumber = 3,
                                    nameOfDose = visitType.key,

                                    windowBefore = 1,
                                    daysFromLastDose = 3,
                                    windowAfter = 1
                                ),
                                fourthFollowUpVisit,
                            )
                        )
                        // Act
                        val upcomingVisit = getUpcomingVisit()
                        // Assert
                        upcomingVisit shouldBe fourthFollowUpVisit.toUpcomingVisit(dosingVisit)
                    }

                }
            }
        }
    }
})
